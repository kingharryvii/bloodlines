package com.harryskingdom.bloodlines.race;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fae's flight is real vanilla creative-style flight (Abilities.flying/mayfly, toggled by vanilla's own
 * double-tap-jump input - no custom trigger code needed), but gated by a stamina-like resource so it can't just
 * be flown forever: drains while actively flying, regenerates while not. Modeled directly on Medieval Origins
 * Revival's own Pixie race (data/medievalorigins/powers/pixie/flight.json), which uses this same resource-gated
 * creative-flight approach rather than anything Icarus-based - deliberately not the same as Icarus's own
 * "superman style" double-jump elytra-glide flight.
 */
public final class RaceFlightResource
{
    public static final int MIN = 0;
    public static final int MAX = 150;
    public static final int START = 100;
    private static final int DRAIN_INTERVAL_TICKS = 5;
    private static final int REGEN_INTERVAL_TICKS = 10;

    private static final Map<UUID, Integer> RESOURCE = new ConcurrentHashMap<>();

    private RaceFlightResource() {}

    public static int get(ServerPlayer player)
    {
        return RESOURCE.getOrDefault(player.getUUID(), START);
    }

    public static void set(ServerPlayer player, int value)
    {
        RESOURCE.put(player.getUUID(), Math.max(MIN, Math.min(MAX, value)));
    }

    public static void clear(ServerPlayer player)
    {
        RESOURCE.remove(player.getUUID());
    }

    /** Called every tick for Fae players: drains while flying, regenerates while not, cuts flight when empty. */
    public static void tick(ServerPlayer player)
    {
        boolean flying = player.getAbilities().flying;

        if (flying)
        {
            if (player.tickCount % DRAIN_INTERVAL_TICKS == 0)
                set(player, get(player) - 1);

            if (get(player) <= MIN)
            {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.displayClientMessage(Component.literal("Out of flight stamina!"), true);
            }
        }
        else if (player.tickCount % REGEN_INTERVAL_TICKS == 0)
        {
            set(player, get(player) + 1);
        }
    }
}
