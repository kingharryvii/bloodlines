package com.harryskingdom.bloodlines.race;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks whether each player on a flying race is currently holding jump. Synced explicitly from the client via
 * a dedicated packet rather than read off vanilla's own jumping field, so it stays reliable regardless of
 * whatever else is going on with that field. Used for Hover's vertical control - double-jump flight itself is
 * handled entirely by the real Icarus mod now, which watches its own jump input independently.
 */
public final class RaceHoverState
{
    private static final Map<UUID, Boolean> JUMPING = new ConcurrentHashMap<>();

    private RaceHoverState() {}

    public static void setJumping(ServerPlayer player, boolean jumping)
    {
        JUMPING.put(player.getUUID(), jumping);
    }

    public static boolean isJumping(ServerPlayer player)
    {
        return JUMPING.getOrDefault(player.getUUID(), false);
    }

    public static void clear(ServerPlayer player)
    {
        JUMPING.remove(player.getUUID());
    }
}
