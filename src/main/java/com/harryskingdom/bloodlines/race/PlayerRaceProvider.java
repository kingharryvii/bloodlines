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
    private LazyOptional<IPlayerRace> lazyOptional = LazyOptional.of(() -> playerRace);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
    {
        return cap == PlayerRaceCapability.PLAYER_RACE ? lazyOptional.cast() : LazyOptional.empty();
    }

    /**
     * This is the real cause behind "lost my race/abilities after changing dimension, and after that even death
     * and relogging couldn't bring it back" - traced through three separate decompiled methods, not guessed.
     * Entity#remove(RemovalReason) unconditionally calls invalidateCaps() for ANY removal, not just death -
     * including the CHANGED_DIMENSION reason ServerPlayer#changeDimension() uses on a player entity that isn't
     * being discarded, just moved to a different level. CapabilityProvider#invalidateCaps() then calls
     * CapabilityDispatcher.invalidate(), which invalidates every registered LazyOptional - permanently, by
     * design (a LazyOptional can never become valid again once invalidated, that's fundamental to the class).
     * changeDimension() does call Entity#revive() on the same persisting entity right afterward, and revive()
     * does call reviveCaps() - but CapabilityProvider#reviveCaps() only flips an internal "valid" boolean back to
     * true. It has no way to touch OUR already-dead LazyOptional object, since that object was ours to create,
     * not the engine's. Before this fix, getCapability() kept returning that same permanently-dead LazyOptional
     * forever after a player's first dimension change of a session - every ifPresent() on it silently did
     * nothing, no exception, nothing in any log, which is exactly why this looked like a resync/timing problem
     * (the class doc on PlayerRaceEvents' respawn/dimension-change handlers) rather than what it actually was.
     * It also explains the death-after-that symptom: onDeath's own snapshot read from this same dead capability,
     * so it silently captured nothing, meaning the post-respawn player started completely blank - and eventually
     * an autosave writes that blank state to disk, which is why relogging popped the race-selection screen too.
     * The fix: recreate the LazyOptional here, in the same invalidate() callback Forge already calls for us, so
     * the *next* getCapability() call - once reviveCaps() flips valid back to true - returns a fresh, working
     * LazyOptional wrapping the same underlying PlayerRace object, instead of the dead original.
     */
    public void invalidate()
    {
        lazyOptional.invalidate();
        lazyOptional = LazyOptional.of(() -> playerRace);
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

        tag.putBoolean("PassiveVisualsEnabled", playerRace.isPassiveVisualsEnabled());

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        // Race.valueOf throws for a saved constant that no longer exists (e.g. a removed race) - fall back to
        // "no race chosen" for that one entry instead of crashing the player's data load entirely.
        if (tag.contains("Race"))
            tryParseRace(tag.getString("Race")).ifPresent(playerRace::setRace);

        if (tag.contains("Unlocked"))
        {
            ListTag unlocked = tag.getList("Unlocked", Tag.TAG_STRING);
            for (int i = 0; i < unlocked.size(); i++)
                tryParseRace(unlocked.getString(i)).ifPresent(playerRace::unlockRace);
        }

        // Missing (older save, or never toggled off) defaults to true via getBoolean's own missing-key behavior.
        if (tag.contains("PassiveVisualsEnabled"))
            playerRace.setPassiveVisualsEnabled(tag.getBoolean("PassiveVisualsEnabled"));
    }

    /**
     * Wood Elf, High Elf and Moon Elf were merged into one combined Elf race - checked by name, not enum
     * constant, since the old constants no longer exist to reference at all (Race.valueOf() would just throw
     * IllegalArgumentException for them, same as any other unrecognized string). Migrating explicitly here means
     * a player who had one of the three picked lands on the new race automatically on next load, rather than
     * falling through to tryParseRace's own "unrecognized name -> no race chosen" fallback and landing back on
     * the race-selection screen.
     */
    private static java.util.Optional<Race> tryParseRace(String name)
    {
        if (name.equals("WOOD_ELF") || name.equals("HIGH_ELF") || name.equals("MOON_ELF"))
            return java.util.Optional.of(Race.ELF);

        try
        {
            return java.util.Optional.of(Race.valueOf(name));
        }
        catch (IllegalArgumentException e)
        {
            return java.util.Optional.empty();
        }
    }
}
