package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.integration.icarus.IcarusIntegration;
import com.harryskingdom.bloodlines.integration.pehkui.PehkuiIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** Applies/removes the numeric attribute modifiers and innate effects tied to a player's race. */
public final class RaceEffects
{
    private static final UUID HEALTH_ID = UUID.fromString("b3d1f8b0-1a2e-4b3d-9c5e-1f2a3b4c5d6e");
    private static final UUID SPEED_ID = UUID.fromString("c4e2f9c1-2b3f-5c4e-ad6f-2a3b4c5d6e7f");
    private static final UUID DAMAGE_ID = UUID.fromString("d5f3f0d2-3c40-6d5f-be70-3a4b5c6d7e80");
    private static final UUID KNOCKBACK_ID = UUID.fromString("e604f1e3-4d51-7e60-cf81-4a5b6c7d8e91");
    private static final UUID LUCK_ID = UUID.fromString("f715f2f4-5e62-8f71-d092-5a6b7c8d9ea2");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("082603f5-6f73-9082-e1a3-6b7c8d9eafb3");

    private RaceEffects() {}

    public static void apply(ServerPlayer player, Race race)
    {
        RaceStats stats = RaceStats.of(race);

        setModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, stats.healthBonus(), AttributeModifier.Operation.ADDITION);
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, stats.speedMultiplier(), AttributeModifier.Operation.MULTIPLY_TOTAL);
        setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, stats.attackDamageBonus(), AttributeModifier.Operation.ADDITION);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, stats.knockbackResistance(), AttributeModifier.Operation.ADDITION);
        setModifier(player, Attributes.LUCK, LUCK_ID, stats.luckBonus(), AttributeModifier.Operation.ADDITION);
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, stats.attackSpeedMultiplier(), AttributeModifier.Operation.MULTIPLY_TOTAL);

        setInnateEffect(player, MobEffects.NIGHT_VISION, stats.nightVision());
        setInnateEffect(player, MobEffects.FIRE_RESISTANCE, stats.fireResistant());
        setInnateEffect(player, MobEffects.SLOW_FALLING, stats.slowFalling());
        setInnateEffect(player, MobEffects.WATER_BREATHING, stats.aquatic());
        setInnateEffect(player, MobEffects.DOLPHINS_GRACE, stats.aquatic());

        if (race == Race.FAE)
            PehkuiIntegration.applyFaeScale(player);
        else
            PehkuiIntegration.resetScale(player);

        IcarusIntegration.updateWings(player, race);
    }

    public static void clear(ServerPlayer player)
    {
        clearModifier(player, Attributes.MAX_HEALTH, HEALTH_ID);
        clearModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        clearModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        clearModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
        clearModifier(player, Attributes.LUCK, LUCK_ID);
        clearModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID);

        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.WATER_BREATHING);
        player.removeEffect(MobEffects.DOLPHINS_GRACE);
        player.removeEffect(MobEffects.LEVITATION);
        RaceHoverState.clear(player);

        PehkuiIntegration.resetScale(player);
        IcarusIntegration.updateWings(player, null);
    }

    private static void setInnateEffect(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, boolean shouldHave)
    {
        if (shouldHave)
            player.addEffect(new MobEffectInstance(effect, 999999, 0, true, false, false));
        else
            player.removeEffect(effect);
    }

    private static void setModifier(ServerPlayer player, Attribute attribute, UUID id, double amount, AttributeModifier.Operation operation)
    {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;

        instance.removeModifier(id);
        if (amount != 0)
            instance.addTransientModifier(new AttributeModifier(id, "harrys_bloodlines:race", amount, operation));
    }

    private static void clearModifier(ServerPlayer player, Attribute attribute, UUID id)
    {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null)
            instance.removeModifier(id);
    }
}
