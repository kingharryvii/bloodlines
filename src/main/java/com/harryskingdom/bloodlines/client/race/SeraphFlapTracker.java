package com.harryskingdom.bloodlines.client.race;

import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side record of "when did entity X last flap", by entity id. Populated immediately (zero-latency) for
 * the local player by SeraphFlightController the instant a flap happens, and for every other visible Seraph by
 * SyncSeraphFlapPacket once the server relays it - either way, SeraphWingsLayer reads the same map to drive the
 * downstroke/recovery animation, so local and remote players animate identically.
 */
public final class SeraphFlapTracker
{
    private static final Map<Integer, Long> LAST_FLAP_TICK = new ConcurrentHashMap<>();

    private SeraphFlapTracker() {}

    public static void recordFlap(int entityId)
    {
        if (Minecraft.getInstance().level != null)
            LAST_FLAP_TICK.put(entityId, Minecraft.getInstance().level.getGameTime());
    }

    /** Ticks since this entity's last recorded flap, or a large number if it's never flapped. */
    public static long ticksSinceFlap(int entityId)
    {
        if (Minecraft.getInstance().level == null)
            return Long.MAX_VALUE / 2;

        Long last = LAST_FLAP_TICK.get(entityId);
        return last == null ? Long.MAX_VALUE / 2 : Minecraft.getInstance().level.getGameTime() - last;
    }

    public static void clear(int entityId)
    {
        LAST_FLAP_TICK.remove(entityId);
    }
}
