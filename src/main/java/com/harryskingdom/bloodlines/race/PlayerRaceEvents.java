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
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Map;
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

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (!event.isWasDeath())
            return;

        event.getOriginal().getCapability(PlayerRaceCapability.PLAYER_RACE).ifPresent(oldData ->
                event.getEntity().getCapability(PlayerRaceCapability.PLAYER_RACE).ifPresent(newData ->
                {
                    oldData.getRace().ifPresent(newData::setRace);
                    oldData.getUnlockedRaces().forEach(newData::unlockRace);

                    if (event.getEntity() instanceof ServerPlayer newPlayer)
                        newData.getRace().ifPresent(race -> RaceEffects.apply(newPlayer, race));
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
