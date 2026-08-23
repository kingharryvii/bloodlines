package com.harryskingdom.bloodlines.race.seraph;

import com.harryskingdom.bloodlines.config.SeraphFlightConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side passive food cost for Seraph just for being airborne, on top of the per-flap cost SeraphFlapPacket
 * already deducts. Flight physics itself is client-authoritative (same as Fae and vanilla creative/elytra
 * flight), so there's no server-side "flying" flag to key off - !onGround() is a reasonable, always-accurate
 * proxy since it's real synced position data.
 */
public final class SeraphFlightFood
{
    private SeraphFlightFood() {}

    public static void tick(ServerPlayer player)
    {
        if (SeraphFlightConfig.INDEFINITE_FLIGHT.get() || player.isCreative() || player.onGround())
            return;

        player.getFoodData().addExhaustion(SeraphFlightConfig.PASSIVE_EXHAUSTION_PER_TICK.get().floatValue());
    }
}
