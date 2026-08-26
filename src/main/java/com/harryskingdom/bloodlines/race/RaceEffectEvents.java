package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
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

    /**
     * "Need for Mobility" - Fae, Angelkin and Demonkin can't wear armor heavier than the configured tier (wings
     * need freedom of movement). Reacts to the equip rather than blocking it outright, same technique used by
     * every other "race can't use X" mod: LivingEquipmentChangeEvent fires after the swap already happened, so a
     * disallowed piece gets immediately handed back and the slot cleared, not prevented up front. The cap is
     * measured directly off the item's own defense value for its slot against the configured tier's, not a
     * hardcoded material list, so it also catches modded armor without needing to know about it.
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        if (event.getSlot().getType() != EquipmentSlot.Type.ARMOR)
            return;

        ItemStack equipped = event.getTo();
        if (!(equipped.getItem() instanceof ArmorItem armor))
            return;

        withRace(player, race ->
        {
            // Per-race, not shared - a race's own health/kit might justify a different cap (e.g. Fae's much
            // lower health than Angelkin/Demonkin), so each gets its own configurable tier.
            BloodlinesConfig.MaxArmorTier maxTier = switch (race)
            {
                case FAE -> BloodlinesConfig.FAE_MAX_ARMOR_TIER.get();
                case SERAPH -> BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.get();
                case DEMON -> BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.get();
                default -> null;
            };
            if (maxTier == null || maxTier == BloodlinesConfig.MaxArmorTier.UNRESTRICTED)
                return;

            if (armor.getDefense() <= maxTier.material().getDefenseForType(armor.getType()))
                return;

            player.setItemSlot(event.getSlot(), ItemStack.EMPTY);
            if (!player.getInventory().add(equipped))
                player.drop(equipped, false);

            player.sendSystemMessage(Component.literal("You need freedom of movement - you can't wear armor heavier than "
                            + maxTier.displayName() + ".")
                    .withStyle(ChatFormatting.RED));
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
                RaceFlightFood.tick(player);
        });
    }

    private static void withRace(ServerPlayer player, java.util.function.Consumer<Race> action)
    {
        PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(action));
    }
}
