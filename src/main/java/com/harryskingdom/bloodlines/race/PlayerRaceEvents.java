package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.OpenBloodlineScreenPacket;
import com.harryskingdom.bloodlines.network.SyncPlayerRacePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID)
public class PlayerRaceEvents
{
    private static final ResourceLocation PLAYER_RACE_ID = new ResourceLocation(BloodlinesMod.MODID, "player_race");

    // A third-party mod's own capability (e.g. Pehkui's scale data) isn't guaranteed ready the instant
    // PlayerLoggedInEvent fires. Re-applying race effects a short delay after login is a cheap, safe way to
    // defend against that race condition, since RaceEffects.apply is idempotent.
    private static final int REAPPLY_DELAY_TICKS = 20;
    private static final Map<UUID, Integer> pendingReapply = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            PlayerRaceProvider provider = new PlayerRaceProvider();
            event.addCapability(PLAYER_RACE_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    /**
     * A snapshot of the dying player's race data, taken from LivingDeathEvent - well before PlayerList#respawn()
     * ever runs, since Entity#remove() (called on the old player mid-respawn, before the new one even exists)
     * unconditionally calls invalidateCaps(). By the time PlayerEvent.Clone actually fires - inside
     * ServerPlayer#restoreFrom(), itself called after the old player has already been discarded - the ORIGINAL
     * player's own capability is already invalidated, so event.getOriginal().getCapability(...) silently
     * resolves to nothing and the old onPlayerClone (which read straight from that live capability) quietly did
     * NOTHING on every single death: no race copied to the new player, no RaceEffects.apply() call, no client
     * resync. That's what was behind "died and lost my abilities/water breathing" - not a fluke from mid-edit
     * testing (the original hypothesis when this was first reported), a real bug hit on every death. Confirmed
     * by decompiling PlayerList#respawn() and Entity#remove() directly, not guessed: removePlayerImmediately()
     * (which reaches Entity#remove()) runs before restoreFrom() in respawn()'s own body. Capturing the data
     * here, before any of that invalidation happens, and consuming it in onPlayerClone instead of touching
     * event.getOriginal()'s capability at all sidesteps the timing issue entirely.
     */
    private record RaceSnapshot(Race race, Set<Race> unlockedRaces) {}

    private static final Map<UUID, RaceSnapshot> deathSnapshots = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        PlayerRaceCapability.get(player).ifPresent(data ->
                data.getRace().ifPresent(race ->
                        deathSnapshots.put(player.getUUID(), new RaceSnapshot(race, EnumSet.copyOf(data.getUnlockedRaces())))));
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (!event.isWasDeath())
            return;

        RaceSnapshot snapshot = deathSnapshots.remove(event.getOriginal().getUUID());
        if (snapshot == null)
            return;

        // Only copy the capability data here - do NOT call RaceEffects.apply()/send the sync packet yet. This
        // handler runs inside ServerPlayer#restoreFrom(), which fires well before PlayerList#respawn() sends
        // ClientboundRespawnPacket. That packet tears down the client's LocalPlayer and builds a fresh one with
        // default (non-flying) Abilities; any abilities packet we send before it is applied to the soon-to-be-
        // discarded old client player and never resynced, so granting Fae's mayfly here silently never reaches
        // the client (server-side state ends up correct, which is why this looked like a client-only glitch).
        // onPlayerRespawn below does the actual RaceEffects.apply(), since PlayerRespawnEvent fires later in the
        // same respawn() call, after the client's new player object already exists.
        event.getEntity().getCapability(PlayerRaceCapability.PLAYER_RACE).ifPresent(newData ->
        {
            newData.setRace(snapshot.race());
            snapshot.unlockedRaces().forEach(newData::unlockRace);
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(race ->
        {
            RaceEffects.apply(player, race);
            BloodlinesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncPlayerRacePacket(player.getId(), race));
        }));
    }

    /**
     * "Died and lost my buffs" had a portal-travel twin: took a nether/end portal, attribute-based race stats
     * (health/speed/damage/knockback/luck/attack-speed - everything RaceEffects grants via
     * AttributeInstance.addTransientModifier) silently stopped applying until a full disconnect/reconnect.
     * Traced through ServerPlayer#changeDimension's actual bytecode, not guessed: that method sends the exact
     * same ClientboundRespawnPacket death/respawn does - which tears down and rebuilds the client's player object
     * the same way - and afterward it DOES correctly resend both a fresh ClientboundPlayerAbilitiesPacket and
     * every active MobEffectInstance (both explicitly present in its own bytecode, in the right order after the
     * respawn packet, with nothing resetting either afterward - confirmed by reading all the way to the method's
     * own return). It does not resend attribute modifiers at all - that sync only fires when something marks an
     * AttributeInstance dirty, and changeDimension() never touches ours.
     * <p>
     * PlayerChangedDimensionEvent fires as the very last step of changeDimension(), which looked like the safe
     * spot to reapply from - but an immediate RaceEffects.apply() here still didn't fix it in testing (confirmed
     * against a real portal, not the /execute in ... run tp command, which turned out to route through a
     * different method - ServerPlayer#teleportTo - that never fires this event at all and was the wrong way to
     * test this in the first place). Most likely cause: the player's own tracked-entity bookkeeping for the new
     * level isn't fully in place yet the instant this event fires, so marking attributes dirty here gets lost
     * once that bookkeeping actually settles a moment later - the same class of "our reapply and the engine's own
     * internal state race each other" problem the respawn fix hit, just one layer deeper this time. Rather than
     * chase the exact tick that race resolves on, this reuses the same delayed-reapply fallback already proven
     * for login (pendingReapply/REAPPLY_DELAY_TICKS, originally added for third-party capabilities like Pehkui
     * not being ready instantly) - by the time it fires a second later, everything has settled.
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(race -> RaceEffects.apply(player, race)));
        pendingReapply.put(player.getUUID(), REAPPLY_DELAY_TICKS);
    }

    /**
     * Leaving Creative mode (back to Survival) also loses Fae's flight, for a related but distinct reason from
     * the other two - traced through ServerPlayer#setGameMode(GameType) and ForgeHooks#onChangeGameType, not
     * guessed. setGameMode() fires PlayerChangeGameModeEvent first - it's a pre-change, cancellable event (you
     * can call setNewGameMode() on it, or cancel it outright, which is only possible before the change happens) -
     * and only afterward calls ServerPlayerGameMode#changeGameModeForPlayer(), which is what actually calls
     * GameType.updatePlayerAbilities() and resets mayfly/flying to Survival's defaults (the exact same vanilla
     * mechanism behind the very first bug this session, just triggered by a mode switch instead of a respawn).
     * There's no Forge event that fires AFTER that reset completes - setGameMode() just returns once its own
     * cleanup (onUpdateAbilities(), updateEffectVisibility()) is done, nothing further to hook. Calling
     * RaceEffects.apply() directly from this event would run before the reset and get immediately undone by it,
     * the same "our reapply and the engine's own internal state race each other" problem as the dimension-change
     * fix - so this queues the same delayed reapply instead of trying to react synchronously. Harmless when
     * switching INTO creative too: RaceEffects.setMayFly() already no-ops for isCreative()/isSpectator() players,
     * so the delayed reapply just does nothing useful until they actually leave creative again.
     */
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        pendingReapply.put(player.getUUID(), REAPPLY_DELAY_TICKS);
    }

    // Curios used to sync equipped wing items to observers for free; race isn't backed by an item any more, so
    // we sync it ourselves whenever a new client starts tracking a player (i.e. that player just came into view).
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event)
    {
        if (!(event.getTarget() instanceof ServerPlayer tracked) || !(event.getEntity() instanceof ServerPlayer observer))
            return;

        PlayerRaceCapability.get(tracked).ifPresent(data -> data.getRace().ifPresent(race ->
                BloodlinesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer),
                        new SyncPlayerRacePacket(tracked.getId(), race))));
    }

    /**
     * RaceEffects.apply() is NOT called synchronously here on purpose - traced a server watchdog crash to this
     * exact spot: a player logging in while the server was still actively generating new terrain (bettercaves
     * mid-generation) triggered PehkuiIntegration.resetScale() -> Pehkui's own ScaleData.resetScale(), which does
     * a ground/collision check that needed a chunk that wasn't finished generating yet, stalling the entire main
     * thread long enough to trip the watchdog. Login is the one place in this class where a fresh player joining
     * a not-yet-loaded area is actually likely, unlike respawn/dimension-change/game-mode-switch which normally
     * happen well after someone's already been playing in an already-loaded area. The client-facing race sync
     * packet still goes out immediately below - only the potentially-blocking Pehkui call moves onto the same
     * tick-delayed pendingReapply path onPlayerChangeGameMode already uses safely for the same "don't call a
     * third-party mod synchronously from inside this event" reason.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        PlayerRaceCapability.get(player).ifPresent(data ->
        {
            if (data.hasChosenRace())
            {
                data.getRace().ifPresent(race ->
                        BloodlinesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                                new SyncPlayerRacePacket(player.getId(), race)));
                pendingReapply.put(player.getUUID(), REAPPLY_DELAY_TICKS);
            }
            else
                BloodlinesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenBloodlineScreenPacket());
        });
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || pendingReapply.isEmpty())
            return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        for (UUID id : new ArrayList<>(pendingReapply.keySet()))
        {
            int remaining = pendingReapply.merge(id, -1, Integer::sum);
            if (remaining > 0)
                continue;

            pendingReapply.remove(id);

            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null)
                PlayerRaceCapability.get(player).ifPresent(data ->
                        data.getRace().ifPresent(race -> RaceEffects.apply(player, race)));
        }
    }
}
