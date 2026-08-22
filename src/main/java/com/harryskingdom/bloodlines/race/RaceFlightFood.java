package com.harryskingdom.bloodlines.race;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fae's flight is real vanilla creative-style flight (Abilities.flying/mayfly, toggled by vanilla's own
 * double-tap-jump input), gated by hunger instead of a custom stamina resource - the same approach Icarus itself
 * offers as an alternative to its stamina attribute (see IcarusHelper.canFly and ApplyBoostPacket in their
 * source): a minimum food level required to fly at all, and food exhaustion added while actively thrusting
 * forward during flight (not just while airborne), at the same threshold and rate Icarus defaults to. Recovery
 * is entirely vanilla's own hunger/saturation regen - there's no separate resource or per-player state to track
 * here.
 */
public final class RaceFlightFood
{
    private static final int REQUIRED_FOOD_LEVEL = 7;
    private static final float EXHAUSTION_PER_BOOST_TICK = 0.03F;

    private RaceFlightFood() {}

    /** Called every tick for Fae players: cuts flight if too hungry, otherwise drains food while thrusting forward. */
    public static void tick(ServerPlayer player)
    {
        if (!player.getAbilities().flying || player.isCreative())
            return;

        if (player.getFoodData().getFoodLevel() < REQUIRED_FOOD_LEVEL)
        {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.displayClientMessage(Component.literal("Too hungry to fly!"), true);
            return;
        }

        if (player.zza > 0)
            player.getFoodData().addExhaustion(EXHAUSTION_PER_BOOST_TICK);
    }
}
