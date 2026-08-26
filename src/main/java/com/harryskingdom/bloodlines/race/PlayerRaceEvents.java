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
                {
                    RaceEffects.apply(player, race);
                    BloodlinesNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncPlayerRacePacket(player.getId(), race));
                });
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
