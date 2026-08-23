package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.config.SeraphFlightConfig;
import com.harryskingdom.bloodlines.race.PlayerRaceCapability;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.RaceAbilityCooldowns;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S: sent by the client the moment a Seraph flap actually happens (matching the same pattern Icarus itself
 * uses for its own boost packet). The server is the authority on food cost and is a cooldown backstop against a
 * modified client just spamming this - the client already self-throttles via its own cooldown, this just makes
 * sure a client can't skip it. On success, relays a sync packet to everyone tracking this player so their wings
 * animate correctly in third person too.
 */
public class SeraphFlapPacket
{
    public static void encode(SeraphFlapPacket msg, FriendlyByteBuf buf) {}

    public static SeraphFlapPacket decode(FriendlyByteBuf buf)
    {
        return new SeraphFlapPacket();
    }

    public static void handle(SeraphFlapPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;

            PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(race ->
            {
                if (race != Race.SERAPH)
                    return;

                if (!RaceAbilityCooldowns.isReady(player, SeraphFlightConfig.FLAP_COOLDOWN_TICKS.get()))
                    return;

                boolean indefinite = SeraphFlightConfig.INDEFINITE_FLIGHT.get();
                if (!indefinite && !player.isCreative())
                {
                    if (player.getFoodData().getFoodLevel() < SeraphFlightConfig.REQUIRED_FOOD_LEVEL.get())
                        return;

                    player.getFoodData().addExhaustion(SeraphFlightConfig.FLAP_EXHAUSTION_COST.get().floatValue());
                }

                RaceAbilityCooldowns.markUsed(player);

                BloodlinesNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new SyncSeraphFlapPacket(player.getId()));
            }));
        });
        ctx.setPacketHandled(true);
    }
}
