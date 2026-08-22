package com.harryskingdom.bloodlines.race;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only cache of other entities' chosen race, populated by SyncPlayerRacePacket. Render layers use this to
 * decide whether to draw racial wings on an arbitrary LivingEntity (including other players), since that can no
 * longer be inferred from an equipped Curios item now that flight/wings don't go through Curios.
 */
public final class ClientRaceCache
{
    private static final Map<Integer, Race> RACES = new ConcurrentHashMap<>();

    private ClientRaceCache() {}

    public static void set(int entityId, String raceId)
    {
        try
        {
            RACES.put(entityId, Race.valueOf(raceId.toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException e)
        {
            RACES.remove(entityId);
        }
    }

    public static Race get(int entityId)
    {
        return RACES.get(entityId);
    }
}
