package com.harryskingdom.bloodlines.race;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class PlayerRace implements IPlayerRace
{
    private Race race;
    private final Set<Race> unlockedRaces = EnumSet.noneOf(Race.class);

    @Override
    public Optional<Race> getRace()
    {
        return Optional.ofNullable(race);
    }

    @Override
    public void setRace(Race race)
    {
        this.race = race;
    }

    @Override
    public boolean hasChosenRace()
    {
        return race != null;
    }

    @Override
    public Set<Race> getUnlockedRaces()
    {
        return unlockedRaces;
    }

    @Override
    public void unlockRace(Race race)
    {
        unlockedRaces.add(race);
    }

    @Override
    public boolean canSelect(Race race)
    {
        return !race.isLocked() || unlockedRaces.contains(race);
    }
}
