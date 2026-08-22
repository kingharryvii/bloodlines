package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.BloodlinesMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID)
public class RaceEffectEvents
{
    @SubscribeEvent
    public static void onFall(LivingFallEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        withRace(player, race ->
        {
            RaceStats stats = RaceStats.of(race);

            if (stats.noFallDamage())
                event.setDamageMultiplier(0f);
            else if (stats.fallDamageReductionPercent() > 0)
                event.setDamageMultiplier((float) (event.getDamageMultiplier() * (1 - stats.fallDamageReductionPercent())));
        });
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        withRace(player, race ->
        {
            double multiplier = RaceStats.of(race).miningSpeedMultiplier();
            if (multiplier != 0)
                event.setNewSpeed((float) (event.getNewSpeed() * (1 + multiplier)));
        });
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event)
    {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;

        withRace(player, race ->
        {
            RaceStats stats = RaceStats.of(race);

            if (event.getSource().getDirectEntity() instanceof AbstractArrow && stats.bowDamageMultiplier() != 0)
                event.setAmount((float) (event.getAmount() * (1 + stats.bowDamageMultiplier())));

            if (stats.lifestealPercent() > 0)
                player.heal((float) (event.getAmount() * stats.lifestealPercent()));
        });
    }

    /** Undead-coded races aren't targeted by hostile undead mobs — undead solidarity. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event)
    {
        if (!(event.getNewTarget() instanceof ServerPlayer player))
            return;

        if (event.getEntity().getMobType() != MobType.UNDEAD)
            return;

        withRace(player, race ->
        {
            if (RaceStats.of(race).undead())
                event.setCanceled(true);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        if (!(event.player instanceof ServerPlayer player))
            return;

        withRace(player, race ->
        {
            if (race == Race.FAE)
                RaceFlightResource.tick(player);
        });
    }

    private static void withRace(ServerPlayer player, java.util.function.Consumer<Race> action)
    {
        PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(action));
    }
}
