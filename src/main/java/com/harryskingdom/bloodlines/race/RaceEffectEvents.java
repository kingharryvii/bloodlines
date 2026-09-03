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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

            if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow)
            {
                double projectileBonus = stats.bowDamageMultiplier() + RaceWeaponAffinity.crossbowBonusFor(race, arrow.shotFromCrossbow());
                if (projectileBonus != 0)
                    event.setAmount((float) (event.getAmount() * (1 + projectileBonus)));
            }
            // Melee weapon affinity - only for a direct hand-to-hand hit (the arrow/bolt case above already
            // covers the projectile side), so a race's weapon bonus doesn't also creep onto thrown/shot damage.
            else if (event.getSource().getDirectEntity() == player)
            {
                double weaponBonus = RaceWeaponAffinity.bonusFor(race, player.getMainHandItem());
                if (weaponBonus != 0)
                    event.setAmount((float) (event.getAmount() * (1 + weaponBonus)));
            }

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
     * Beastkin scare creepers away - a predator-instinct nod matching the "animal-blooded" theme (their own
     * senses read as a threat before a creeper ever gets close enough to fuse). Unlike the undead neutrality
     * above, there's no "unless attacked first" exception - a creeper doesn't have a retaliation goal the way
     * undead do (it just walks up and explodes), so there's no equivalent case to carve back out here.
     */
    @SubscribeEvent
    public static void onCreeperTarget(LivingChangeTargetEvent event)
    {
        if (!(event.getNewTarget() instanceof ServerPlayer player))
            return;

        if (!(event.getEntity() instanceof Creeper))
            return;

        withRace(player, race ->
        {
            if (race == Race.BEASTKIN)
                event.setCanceled(true);
        });
    }

    /** Past this range, a sneaking Shadowkin can't be freshly targeted at all - see onStealthTarget below. */
    private static final double STEALTH_RANGE_SQ = 5.0 * 5.0;

    /**
     * Zeroes out gravity's contribution while a Merfolk is in water - same ForgeMod.ENTITY_GRAVITY attribute,
     * and the same add/remove-a-transient-modifier technique, vanilla's own LivingEntity#travel() already uses
     * for Slow Falling (confirmed by decompile). MULTIPLY_TOTAL at -1.0 always zeroes the attribute's final
     * value regardless of base, and coexists cleanly with vanilla's own Slow Falling modifier since that one is
     * ADDITION, not MULTIPLY_TOTAL, so the two never fight over the same operation. Without this, an idle
     * Merfolk sinks toward the seafloor exactly like a landlubber would - vanilla's own swim movement still
     * subtracts a small per-tick gravity pull even while submerged (getFluidFallingAdjustedMovement) - which
     * reads as a natural sea creature not actually being at home in the water. Everything else about swimming
     * (horizontal movement, Dolphin's Grace-style drag, actively swimming up/down) is untouched - only the
     * passive downward drift while otherwise idle goes away.
     */
    private static final AttributeModifier MERFOLK_BUOYANCY = new AttributeModifier(
            UUID.fromString("6a3f9b2e-6c2b-4a3a-8a7d-0e6c1b8f2a4d"), "Merfolk buoyancy", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);

    /**
     * Shadowkin's "Unseen" - hostile mobs have a much harder time noticing you while sneaking, past melee
     * range; get close while crawling and you can still be spotted, same as vanilla sneaking already reduces
     * detection without erasing it outright. Standing up makes you visible to searches again immediately.
     * Doesn't interrupt an already-engaged fight - a mob that already has this player as its own
     * Mob#getLastHurtByMob() (the same "already retaliating" read the undead-neutrality handler above uses)
     * keeps its target rather than an ambush suddenly going unnoticed mid-fight - and boss mobs are excluded
     * outright so a boss fight can't be cheesed by crouching.
     */
    @SubscribeEvent
    public static void onStealthTarget(LivingChangeTargetEvent event)
    {
        if (!(event.getNewTarget() instanceof ServerPlayer player) || !player.isCrouching())
            return;

        if (event.getEntity() instanceof WitherBoss || event.getEntity() instanceof EnderDragon)
            return;

        if (event.getEntity() instanceof Mob mob && mob.getLastHurtByMob() == player)
            return;

        if (event.getEntity().distanceToSqr(player) <= STEALTH_RANGE_SQ)
            return;

        withRace(player, race ->
        {
            if (race == Race.GOBLIN)
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

    /**
     * Elf potion affinity - a thrown splash/lingering potion's effects last 25% longer when an Elf threw it.
     * Fires before ThrownPotion#onHit() reads the item's effects (ProjectileImpactEvent fires at the top of
     * Projectile#onHit(), confirmed by decompile, ahead of ThrownPotion's own onHit() override that calls
     * PotionUtils.getMobEffects(this.getItem()) to decide what to apply), so replacing the entity's item here
     * changes what actually lands. PotionUtils.getMobEffects() already merges the base potion type's own
     * effects together with any custom ones into one list - re-flattening that merged, duration-boosted list
     * back onto the stack as custom effects while resetting the potion type to Potions.EMPTY (so
     * getAllEffects() doesn't also re-add the original, unboosted base-potion effects on top) avoids the
     * result carrying the same effects twice.
     */
    @SubscribeEvent
    public static void onPotionImpact(ProjectileImpactEvent event)
    {
        if (!(event.getEntity() instanceof ThrownPotion potion))
            return;

        if (!(potion.getOwner() instanceof ServerPlayer player))
            return;

        withRace(player, race ->
        {
            if (race != Race.ELF)
                return;

            List<MobEffectInstance> boosted = new ArrayList<>();
            for (MobEffectInstance effect : PotionUtils.getMobEffects(potion.getItem()))
            {
                boosted.add(new MobEffectInstance(effect.getEffect(), (int) (effect.getDuration() * 1.25),
                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }

            ItemStack boostedStack = potion.getItem().copy();
            PotionUtils.setPotion(boostedStack, Potions.EMPTY);
            PotionUtils.setCustomEffects(boostedStack, boosted);
            potion.setItem(boostedStack);
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

                AttributeInstance gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravity != null)
                {
                    if (player.isInWater())
                    {
                        if (!gravity.hasModifier(MERFOLK_BUOYANCY))
                            gravity.addTransientModifier(MERFOLK_BUOYANCY);
                    }
                    else if (gravity.hasModifier(MERFOLK_BUOYANCY))
                        gravity.removeModifier(MERFOLK_BUOYANCY);
                }

                // Belt-and-suspenders on top of the gravity modifier above: floors any residual downward
                // velocity to 0 rather than trusting the attribute alone to reach exactly zero every tick (the
                // gravity fix still visibly left a very slow sink in testing, and setDeltaMovement() here is
                // itself synced back to the client the same way server-driven knockback/explosions already are,
                // so this reliably corrects it rather than fighting client-side prediction). Skipped while
                // crouching - crouch-to-dive is the same intentional "swim downward" input vanilla's own water
                // controls use, so a Merfolk actively diving still sinks exactly as fast as they choose to.
                if (player.isInWater() && !player.isCrouching() && player.getDeltaMovement().y < 0)
                {
                    Vec3 velocity = player.getDeltaMovement();
                    player.setDeltaMovement(velocity.x, 0, velocity.z);
                }
            }

            // Demonkin move noticeably faster through lava than vanilla's own thick, wading-speed crawl - not a
            // real swim mechanic (confirmed by decompiling LivingEntity's own fluid movement dispatch: it
            // explicitly skips moveInFluid(), the method that handles water's swim-style movement, whenever the
            // fluid type is lava - lava is a deliberately separate, slower code path in vanilla/Forge, not just
            // water with a bigger drag constant, so there's no "Dolphin's Grace for lava" to lean on). This tops
            // horizontal speed up to roughly normal walking speed whenever lava's own drag has dragged it below
            // that, every tick, rather than granting true swimming.
            if (race == Race.DEMON && player.isInLava())
            {
                Vec3 velocity = player.getDeltaMovement();
                double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                double targetSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

                if (horizontalSpeed > 0 && horizontalSpeed < targetSpeed)
                {
                    double scale = targetSpeed / horizontalSpeed;
                    player.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
                }
            }
        });
    }

    private static void withRace(ServerPlayer player, java.util.function.Consumer<Race> action)
    {
        PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(action));
    }
}
