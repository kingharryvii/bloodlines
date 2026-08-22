package com.harryskingdom.bloodlines.race;

import net.minecraft.server.level.ServerPlayer;

public final class RaceSelection
{
    private RaceSelection() {}

    public enum Result
    {
        SUCCESS,
        ALREADY_CHOSEN,
        LOCKED
    }

    public static Result select(ServerPlayer player, Race race)
    {
        IPlayerRace data = PlayerRaceCapability.get(player).orElseThrow(() -> new IllegalStateException("Player race capability missing"));

        if (data.hasChosenRace())
            return Result.ALREADY_CHOSEN;

        if (!data.canSelect(race))
            return Result.LOCKED;

        data.setRace(race);
        RaceEffects.apply(player, race);
        return Result.SUCCESS;
    }
}
