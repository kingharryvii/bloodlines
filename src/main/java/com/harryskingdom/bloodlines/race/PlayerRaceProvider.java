package com.harryskingdom.bloodlines.race;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerRaceProvider implements ICapabilitySerializable<CompoundTag>
{
    private final PlayerRace playerRace = new PlayerRace();
    private final LazyOptional<IPlayerRace> lazyOptional = LazyOptional.of(() -> playerRace);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
    {
        return cap == PlayerRaceCapability.PLAYER_RACE ? lazyOptional.cast() : LazyOptional.empty();
    }

    public void invalidate()
    {
        lazyOptional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        playerRace.getRace().ifPresent(race -> tag.putString("Race", race.name()));

        ListTag unlocked = new ListTag();
        for (Race race : playerRace.getUnlockedRaces())
            unlocked.add(StringTag.valueOf(race.name()));
        tag.put("Unlocked", unlocked);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if (tag.contains("Race"))
            playerRace.setRace(Race.valueOf(tag.getString("Race")));

        if (tag.contains("Unlocked"))
        {
            ListTag unlocked = tag.getList("Unlocked", Tag.TAG_STRING);
            for (int i = 0; i < unlocked.size(); i++)
                playerRace.unlockRace(Race.valueOf(unlocked.getString(i)));
        }
    }
}
