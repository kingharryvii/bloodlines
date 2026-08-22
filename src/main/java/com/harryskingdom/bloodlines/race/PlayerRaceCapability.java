package com.harryskingdom.bloodlines.race;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public final class PlayerRaceCapability
{
    public static final Capability<IPlayerRace> PLAYER_RACE = CapabilityManager.get(new CapabilityToken<>() {});

    private PlayerRaceCapability() {}

    public static LazyOptional<IPlayerRace> get(Player player)
    {
        return player.getCapability(PLAYER_RACE);
    }
}
