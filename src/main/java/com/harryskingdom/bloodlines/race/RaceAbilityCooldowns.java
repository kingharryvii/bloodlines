package com.harryskingdom.bloodlines.race;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player racial ability cooldowns in memory. Resets on relog, which is fine for a cooldown.
 * Primary and secondary abilities cool down independently - two separate maps rather than one, so using one
 * doesn't hold the other one hostage.
 */
public final class RaceAbilityCooldowns
{
    private static final Map<UUID, Long> LAST_USED_PRIMARY = new HashMap<>();
    private static final Map<UUID, Long> LAST_USED_SECONDARY = new HashMap<>();

    private RaceAbilityCooldowns() {}

    public static boolean isReady(ServerPlayer player, int cooldownTicks)
    {
        return isReady(LAST_USED_PRIMARY, player, cooldownTicks);
    }

    public static int ticksRemaining(ServerPlayer player, int cooldownTicks)
    {
        return ticksRemaining(LAST_USED_PRIMARY, player, cooldownTicks);
    }

    public static void markUsed(ServerPlayer player)
    {
        LAST_USED_PRIMARY.put(player.getUUID(), player.level().getGameTime());
    }

    public static boolean isSecondaryReady(ServerPlayer player, int cooldownTicks)
    {
        return isReady(LAST_USED_SECONDARY, player, cooldownTicks);
    }

    public static int secondaryTicksRemaining(ServerPlayer player, int cooldownTicks)
    {
        return ticksRemaining(LAST_USED_SECONDARY, player, cooldownTicks);
    }

    public static void markSecondaryUsed(ServerPlayer player)
    {
        LAST_USED_SECONDARY.put(player.getUUID(), player.level().getGameTime());
    }

    private static boolean isReady(Map<UUID, Long> lastUsed, ServerPlayer player, int cooldownTicks)
    {
        Long last = lastUsed.get(player.getUUID());
        return last == null || player.level().getGameTime() - last >= cooldownTicks;
    }

    private static int ticksRemaining(Map<UUID, Long> lastUsed, ServerPlayer player, int cooldownTicks)
    {
        Long last = lastUsed.get(player.getUUID());
        if (last == null)
            return 0;

        return (int) Math.max(0, cooldownTicks - (player.level().getGameTime() - last));
    }
}
