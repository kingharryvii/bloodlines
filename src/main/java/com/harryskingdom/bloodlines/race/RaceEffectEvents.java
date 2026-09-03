package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
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
            RaceStats stats = RaceStats.of(race);

            double multiplier = stats.miningSpeedMultiplier();
            if (multiplier != 0)
                event.setNewSpeed((float) (event.getNewSpeed() * (1 + multiplier)));

            // Built-in Aqua Affinity for aquatic races - Player#getDigSpeed() divides speed by 5 when the eyes
            // are in water and there's no Aqua Affinity helmet (confirmed by decompiling it: that division, and
            // a separate one for not being on the ground, both happen before ForgeEventFactory.getBreakSpeed()
            // fires this same event we're already handling above), so undoing it here just means multiplying
            // back by 5 whenever that exact vanilla condition is true. Matches real Aqua Affinity's own scope
            // exactly - the separate off-ground penalty is untouched, same as it would be with an actual
            // enchanted helmet, so mining while freely swimming off the seafloor is still slower than standing
            // on it.
            if (stats.aquatic() && player.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player))
                event.setNewSpeed(event.getNewSpeed() * 5f);
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

            if (stats.undead())
                alertNearbyHorde(event.getEntity(), player);
        });
    }

    /**
     * "Provoked the horde" - an undead-coded player attacking one hostile undead mob alerts any other undead of
     * that exact same type nearby too, so picking a fight with one member of a graveyard cluster turns the whole
     * cluster hostile instead of just the one target. The search radius and AABB shape deliberately mirror
     * vanilla's own HurtByTargetGoal#alertOthers() exactly (confirmed by decompiling it: same
     * AABB.unitCubeFromLowerCorner(pos).inflate(followRange, 10.0, followRange), same Attributes.FOLLOW_RANGE
     * read off the hit mob) rather than a made-up constant, so a horde feels exactly as far-reaching as vanilla's
     * zombies already are to each other (35 blocks) - it's just that vanilla's own alert never actually reaches a
     * player here, since alertOther() only ever calls Mob#setTarget() and never touches getLastHurtByMob(), so
     * onChangeTarget's retaliation-only filter below would otherwise cancel it. Scoped to only whatever's nearby
     * at the moment of the hit, not a persistent per-player flag: each alerted mob just gets handed the same
     * Mob#setLastHurtByMob(player) vanilla's own HurtByTargetGoal already reads to decide who to retaliate
     * against. A mob that wasn't nearby for this fight - including one met later somewhere else entirely - is
     * never touched and stays neutral.
     */
    private static void alertNearbyHorde(LivingEntity hit, ServerPlayer player)
    {
        if (!(hit instanceof Mob hitMob) || hitMob.getMobType() != MobType.UNDEAD || hitMob instanceof WitherBoss)
            return;

        double followRange = hitMob.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB alertArea = AABB.unitCubeFromLowerCorner(hitMob.position()).inflate(followRange, 10.0, followRange);

        hitMob.level().getEntitiesOfClass(hitMob.getClass(), alertArea, other -> other != hitMob && other.isAlive())
                .forEach(other -> other.setLastHurtByMob(player));
    }

    /**
     * Undead-coded races are treated as one of their own by hostile undead - left alone unless you attack first,
     * not immune outright. Explicitly excludes WitherBoss even though it reports MobType.UNDEAD (confirmed by
     * decompiling WitherBoss#getMobType, not assumed) - a boss fight needs to stay a fight regardless of the
     * "stay neutral unless attacked" rule below.
     * <p>
     * "Unless attacked first" is checked via Mob#getLastHurtByMob(), not custom state - confirmed by decompiling
     * HurtByTargetGoal, vanilla's own "retaliate against whoever just hit me" AI goal reads that exact same
     * field to decide who to retaliate against, then calls the same Mob#setTarget() this event fires from. So a
     * LivingChangeTargetEvent where the mob's own getLastHurtByMob() is already this player is, for all practical
     * purposes, always that retaliation goal doing its job - letting it through is what makes "attack first and
     * they fight back" work, while every other target-change attempt (ordinary proximity aggro-searching, never
     * having been hit by this player) still gets cancelled.
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event)
    {
        if (!(event.getNewTarget() instanceof ServerPlayer player))
            return;

        if (event.getEntity().getMobType() != MobType.UNDEAD || event.getEntity() instanceof WitherBoss)
            return;

        if (event.getEntity() instanceof Mob mob && mob.getLastHurtByMob() == player)
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

            // Clear underwater vision for Merfolk, not full always-on night vision - same broad isInWater() check
            // TailLayer/onRenderPlayerPre already gate the tail on. Granted once as genuinely infinite (duration
            // -1, same fix as RaceEffects' own innate effects) rather than repeatedly reapplying a short duration
            // every tick - that earlier approach relied on MobEffectInstance#update() picking up the refresh
            // every single tick without fail, and in practice it didn't: the duration visibly cycled instead of
            // staying flat. hasEffect() guards the grant so it's a one-time add per water-entry, not a per-tick
            // write; removeEffect() cuts it the instant they're no longer in water (a no-op if it wasn't present,
            // e.g. an on-land Merfolk) rather than waiting on an expiry that no longer exists.
            if (race == Race.MERFOLK)
            {
                if (player.isInWater() && RaceEffects.passiveVisualsEnabled(player))
                {
                    if (!player.hasEffect(MobEffects.NIGHT_VISION))
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, true, false, false));
                }
                else
                    player.removeEffect(MobEffects.NIGHT_VISION);
            }
        });
    }

    private static void withRace(ServerPlayer player, java.util.function.Consumer<Race> action)
    {
        PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(action));
    }
}
