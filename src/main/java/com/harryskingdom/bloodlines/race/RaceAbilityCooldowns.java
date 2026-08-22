package com.harryskingdom.bloodlines.race;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks per-player racial ability cooldowns in memory. Resets on relog, which is fine for a cooldown. */
public final class RaceAbilityCooldowns
{
    private static final Map<UUID, Long> LAST_USED = new HashMap<>();

    private RaceAbilityCooldowns() {}

    public static boolean isReady(ServerPlayer player, int cooldownTicks)
    {
        Long last = LAST_USED.get(player.getUUID());
        return last == null || player.level().getGameTime() - last >= cooldownTicks;
    }

    public static int ticksRemaining(ServerPlayer player, int cooldownTicks)
    {
        Long last = LAST_USED.get(player.getUUID());
        if (last == null)
            return 0;

        return (int) Math.max(0, cooldownTicks - (player.level().getGameTime() - last));
    }

    public static void markUsed(ServerPlayer player)
    {
        LAST_USED.put(player.getUUID(), player.level().getGameTime());
    }
}
