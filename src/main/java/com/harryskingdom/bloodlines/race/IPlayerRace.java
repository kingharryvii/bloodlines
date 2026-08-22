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
}
