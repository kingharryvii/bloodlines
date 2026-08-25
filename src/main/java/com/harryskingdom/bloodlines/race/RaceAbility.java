package com.harryskingdom.bloodlines.race;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Each starting race's active, cooldown-gated special move. */
public final class RaceAbility
{
    public static final int COOLDOWN_TICKS = 20 * 45;
    // Was 20*6 (6s) - felt over almost as soon as it landed against a 45s cooldown. 10s raises active uptime
    // from ~13% to ~22% of the cooldown cycle, enough to actually feel like a buff window in a fight rather
    // than a blink-and-it's-gone flash.
    public static final int DURATION_TICKS = 20 * 10;
    /** Range for the two support abilities (Angelkin's heal, Demonkin's damage boost) that reach nearby allies. */
    private static final double PARTY_RADIUS = 8.0;

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
            case FAE -> "Nature's Blessing";
            case GOBLIN -> "Smoke Bomb";
            case BEASTKIN -> "Feral Howl";
            case REVENANT -> "Siphon";
            case GHOUL -> "Undying Resolve";
            case DEMON -> "Infernal Wrath";
            case TROLL -> "Regenerate";
            case MERFOLK -> "Tidal Surge";
            case SERAPH -> "Divine Descent";
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
            // Traced from Medieval Origins Revival's own fae/natures_blessing.json - a nature-themed healing
            // aura that keeps mending nearby allies for its duration (their version also auto-plants seeds and
            // cleanses negative effects, but Bloodlines' abilities are instant MobEffectInstances rather than
            // Medieval's tick-based custom actions, so Regeneration for the party stands in for the same
            // "heals the group over time" feel).
            case FAE ->
            {
                apply(player, new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 1));
                applyToNearby(player, MobEffects.REGENERATION, DURATION_TICKS, 0);
            }
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
            case GHOUL -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 1));
            // Boosts damage for the whole party, not just self - Infernal Wrath rallies everyone nearby, while
            // the speed stays a personal edge.
            case DEMON ->
            {
                apply(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1));
                applyToNearby(player, MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1);
            }
            case TROLL -> apply(player,
                    new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 2),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 0));
            case MERFOLK -> apply(player,
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1),
                    new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 1));
            // Heals the whole party, not just self - a diving strike that doubles as a rally point, the personal
            // combat effects (jump/damage/shield) stay self-only.
            case SERAPH ->
            {
                apply(player,
                        new MobEffectInstance(MobEffects.JUMP, DURATION_TICKS, 3),
                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, DURATION_TICKS, 1),
                        new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 1));
                applyToNearby(player, MobEffects.HEAL, 1, 1);
            }
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

    /** Applies a fresh effect instance to every player within PARTY_RADIUS, including the caster themselves. */
    private static void applyToNearby(ServerPlayer player, MobEffect effect, int duration, int amplifier)
    {
        for (ServerPlayer nearby : player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(PARTY_RADIUS)))
            nearby.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }
}
