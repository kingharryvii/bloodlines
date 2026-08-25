package com.harryskingdom.bloodlines.race;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Which races have real wings (Angelkin, Demonkin) - shared by IcarusWingHooks, which is what actually makes
 * flight work for them (see that class). Side-aware since IcarusHelper's wrapped functions run on both the
 * client (rendering, the flight steering nudge) and the server (the actual flight-trigger check) - the client
 * branch goes through ClientRaceCache via DistExecutor (this project's established pattern, see
 * SyncPlayerRacePacket) rather than the capability directly.
 */
public final class WingedRace
{
    private WingedRace() {}

    public static boolean isWinged(LivingEntity entity)
    {
        return raceOf(entity) != null;
    }

    /** The winged race this entity is playing, or null if they're not a Player or not one of the winged races. */
    public static Race raceOf(LivingEntity entity)
    {
        if (!(entity instanceof Player player))
            return null;

        // LazyOptional#map wraps the mapper's own result in Optional.of(...) internally, which throws if the
        // mapper returns null - a real crash caught in testing, since this ran for every player every tick, and
        // getRace() is empty for anyone who hasn't picked a race yet. resolve() first converts to a plain
        // java.util.Optional, whose flatMap has no such restriction.
        Race race = player.level().isClientSide()
                ? DistExecutor.unsafeRunForDist(() -> () -> ClientRaceCache.get(player.getId()), () -> () -> null)
                : PlayerRaceCapability.get(player).resolve().flatMap(IPlayerRace::getRace).orElse(null);

        return (race == Race.SERAPH || race == Race.DEMON) ? race : null;
    }
}
