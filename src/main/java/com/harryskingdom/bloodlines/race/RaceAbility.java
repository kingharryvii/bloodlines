package com.harryskingdom.bloodlines.race;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Each starting race's active, cooldown-gated special move. */
public final class RaceAbility
{
    public static final int COOLDOWN_TICKS = 20 * 45;
    private static final int DURATION_TICKS = 20 * 6;

    private RaceAbility() {}

    /** Display name of this race's primary ability, or null if it doesn't have one yet. */
    public static String nameFor(Race race)
    {
        return switch (race)
        {
            case HUMAN -> "Second Wind";
            case WOOD_ELF -> "Hunter's Mark";
            case HIGH_ELF -> "Arcane Surge";
            case MOON_ELF -> "Umbral Step";
            case DWARF -> "Stoneskin";
            case ORC -> "Bloodrage";
            case FELINE -> "Pounce";
            case GOBLIN -> "Smoke Bomb";
            case BEASTKIN -> "Feral Howl";
            case REVENANT -> "Siphon";
            case LICH -> "Death's Grasp";
            case GHOUL -> "Undying Resolve";
            case DEMON -> "Infernal Wrath";
            case TROLL -> "Regenerate";
            case MERFOLK -> "Tidal Surge";
            case SERAPH -> "Wing Dive";
            default -> null;
        };
    }

    /** Display name of this race's secondary ability, or null if it doesn't have one. None currently do. */
    public static String secondaryNameFor(Race race)
    {
        return null;
    }

    /** Triggers the race's ability effects on the player. Returns false if this race has no ability. */
    public static boolean activate(ServerPlayer player, Race race)
    {
        switch (race)
        {
            case HUMAN -> apply(player,
                    new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 0));
            case WOOD_ELF -> apply(player,
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 2),
                    new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 1));
            case HIGH_ELF -> apply(player,
                    new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.LUCK, DURATION_TICKS, 2));
            case MOON_ELF -> apply(player,
                    new MobEffectInstance(MobEffects.INVISIBILITY, DURATION_TICKS, 0));
            case DWARF -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 2));
            case ORC -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 0));
            case FELINE -> apply(player,
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 2),
                    new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 2));
            case GOBLIN -> apply(player,
                    new MobEffectInstance(MobEffects.INVISIBILITY, DURATION_TICKS, 0),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1));
            case BEASTKIN -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 0));
            case REVENANT -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.HEAL, 1, 1));
            case LICH -> apply(player,
                    new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 2),
                    new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 1));
            case GHOUL -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 1));
            case DEMON -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1));
            case TROLL -> apply(player,
                    new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 2),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 0));
            case MERFOLK -> apply(player,
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 1));
            case SERAPH -> apply(player,
                    new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 3),
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 1));
            default ->
            {
                return false;
            }
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1f);
        return true;
    }

    private static void apply(ServerPlayer player, MobEffectInstance... effects)
    {
        for (MobEffectInstance effect : effects)
            player.addEffect(effect);
    }
}
