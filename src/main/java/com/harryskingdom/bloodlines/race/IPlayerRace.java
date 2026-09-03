package com.harryskingdom.bloodlines.race;

import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.Optional;
import java.util.Set;

@AutoRegisterCapability
public interface IPlayerRace
{
    Optional<Race> getRace();

    void setRace(Race race);

    boolean hasChosenRace();

    Set<Race> getUnlockedRaces();

    void unlockRace(Race race);

    /** True if this player is allowed to pick the given race right now (starting race, or already unlocked). */
    boolean canSelect(Race race);

    /**
     * Player-controlled toggle (default on) for purely cosmetic/passive racial effects that tint or otherwise
     * change the screen - currently Night Vision (Beastkin/Revenant/Demonkin/Elf's innate grant, and
     * Merfolk's underwater-only grant) and Fae's Slow Falling. Doesn't touch anything mechanical (attribute
     * bonuses, water breathing, fire resistance, etc.) - only the handful of effects a player might want to look
     * at their own screen without.
     */
    boolean isPassiveVisualsEnabled();

    void setPassiveVisualsEnabled(boolean enabled);
}
