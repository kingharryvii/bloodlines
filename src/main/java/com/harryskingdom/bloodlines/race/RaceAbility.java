package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Each starting race's active, cooldown-gated special move. */
public final class RaceAbility
{
    /** Range for the two support abilities (Angelkin's heal, Demonkin's damage boost) that reach nearby allies. */
    private static final double PARTY_RADIUS = 8.0;

    private RaceAbility() {}

    // Default is 45s cooldown / 10s duration (~22% active uptime) - configurable via BloodlinesConfig. Read fresh
    // each call rather than cached, so an admin's config-screen edit takes effect without a restart.
    public static int cooldownTicks()
    {
        return 20 * BloodlinesConfig.PRIMARY_ABILITY_COOLDOWN_SECONDS.get();
    }

    public static int durationTicks()
    {
        return 20 * BloodlinesConfig.PRIMARY_ABILITY_DURATION_SECONDS.get();
    }

    public static int secondaryCooldownTicks()
    {
        return 20 * BloodlinesConfig.SECONDARY_ABILITY_COOLDOWN_SECONDS.get();
    }

    public static int secondaryDurationTicks()
    {
        return 20 * BloodlinesConfig.SECONDARY_ABILITY_DURATION_SECONDS.get();
    }

    /** Display name of this race's primary ability, or null if it doesn't have one yet. */
    public static String nameFor(Race race)
    {
        return switch (race)
        {
            case HUMAN -> "Second Wind";
            case ELF -> "Elven Ward";
            case DWARF -> "Stoneskin";
            case FAE -> "Nature's Blessing";
            case GOBLIN -> "Smoke Bomb";
            case BEASTKIN -> "Feral Howl";
            case REVENANT -> "Siphon";
            case DEMON -> "Infernal Wrath";
            case TROLL -> "Regenerate";
            case MERFOLK -> "Tidal Surge";
            case SERAPH -> "Divine Descent";
            default -> null;
        };
    }

    /** Display name of this race's secondary ability, or null if it doesn't have one. */
    public static String secondaryNameFor(Race race)
    {
        return switch (race)
        {
            case ELF -> "Stormcall";
            default -> null;
        };
    }

    /** Triggers the race's ability effects on the player. Returns false if this race has no ability. */
    public static boolean activate(ServerPlayer player, Race race)
    {
        int durationTicks = durationTicks();

        switch (race)
        {
            case HUMAN -> apply(player,
                    new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 0));
            // Primary abilities are the effect-buff kind every other race already has; Stormcall (the lightning
            // strike) lives on the secondary slot instead, reserved for the flashier, non-buff abilities - see
            // activateSecondary() below. "Elven Ward" leans defensive/sustain to complement Stormcall's own
            // offense, rather than duplicating it.
            case ELF -> apply(player,
                    new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 1),
                    new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 0));
            case DWARF -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 2));
            // Traced from Medieval Origins Revival's own fae/natures_blessing.json - a nature-themed healing
            // aura that keeps mending nearby allies for its duration (their version also auto-plants seeds and
            // cleanses negative effects, but Bloodlines' abilities are instant MobEffectInstances rather than
            // Medieval's tick-based custom actions, so Regeneration for the party stands in for the same
            // "heals the group over time" feel).
            case FAE ->
            {
                apply(player, new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 1));
                applyToNearby(player, MobEffects.REGENERATION, durationTicks, 0);
            }
            case GOBLIN -> apply(player,
                    new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 1));
            // Jump Boost IV added alongside the existing damage/speed/resistance kit - Beastkin's own innate
            // Jump I (RaceEffects, always-on) was the only jump anything on this race, while Angelkin's ability
            // granted a bigger jump than the actual leaping-predator race did. Matches Angelkin's ability-level
            // jump rather than exceeding it, at the user's request - see git history if that balance call changes.
            case BEASTKIN -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 0),
                    new MobEffectInstance(MobEffects.JUMP, durationTicks, 3));
            case REVENANT -> apply(player,
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 1),
                    new MobEffectInstance(MobEffects.HEAL, 1, 1));
            // Boosts damage for the whole party, not just self - Infernal Wrath rallies everyone nearby, while
            // speed and resistance stay a personal edge. Resistance added at the user's request, to give both
            // winged RARE races (Angelkin/Demonkin) a defensive component in their ability, not just offense.
            case DEMON ->
            {
                apply(player,
                        new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 1),
                        new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 1));
                applyToNearby(player, MobEffects.DAMAGE_BOOST, durationTicks, 1);
            }
            case TROLL -> apply(player,
                    new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 2),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 0));
            case MERFOLK -> apply(player,
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, 1),
                    new MobEffectInstance(MobEffects.JUMP, durationTicks, 1));
            // Heals the whole party, not just self - a diving strike that doubles as a rally point, the personal
            // combat effects (damage/shield/resistance) stay self-only. Jump Boost IV swapped for Resistance II
            // at the user's request - having the biggest jump in the game on the one race that already flies,
            // while Beastkin (the actual leaping-predator race) had none in its own ability, didn't sit right.
            case SERAPH ->
            {
                apply(player,
                        new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, 1),
                        new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 1),
                        new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 1));
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

    /** Triggers the race's secondary ability effects. Returns false if this race has no secondary ability. */
    public static boolean activateSecondary(ServerPlayer player, Race race)
    {
        switch (race)
        {
            // Traced from EAW Origins' own seraphstrider/lightning_active.json (used with the author's
            // permission) - a raycast along the player's own look direction, real minecraft:lightning_bolt
            // spawned wherever it lands (a genuine vanilla entity, so it carries its own built-in strike sound
            // and flash automatically, no custom asset needed), with a firework particle trail along the ray
            // leading up to it. setCause(player) attributes the strike properly (kill credit, etc.) rather than
            // it reading as an untriggered natural strike. An instant offensive strike, not a duration/
            // MobEffectInstance buff - matching its own "call down lightning to smite your enemies" framing,
            // and why it lives on the secondary slot rather than alongside the primary buff abilities.
            case ELF ->
            {
                HitResult hit = player.pick(20.0, 1.0F, false);
                Vec3 target = hit.getLocation();
                ServerLevel level = player.serverLevel();

                level.sendParticles(ParticleTypes.FIREWORK, target.x, target.y, target.z, 30, 0.5, 0.5, 0.5, 0.05);

                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null)
                {
                    bolt.moveTo(target.x, target.y, target.z);
                    bolt.setCause(player);
                    level.addFreshEntity(bolt);
                }
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
