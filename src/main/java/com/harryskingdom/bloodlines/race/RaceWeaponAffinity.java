package com.harryskingdom.bloodlines.race;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Per-race melee weapon affinity - a damage bonus for wielding the "right" weapon type for a race's
 * theme (Elf's own bow/crossbow affinity already lives on RaceStats#bowDamageMultiplier, checked
 * against AbstractArrow in RaceEffectEvents#onDamage, since arrows/bolts are a projectile case rather
 * than this class's melee-weapon-in-hand check). instanceof against these vanilla base item classes
 * catches modded weapons too - nearly every Forge weapon mod's swords/axes/tridents still extend them
 * rather than reimplementing Item directly.
 */
public final class RaceWeaponAffinity
{
    private static final double BONUS = 0.15;

    private RaceWeaponAffinity() {}

    /** Bonus melee damage multiplier for this race wielding this weapon (0 if it has no affinity here). */
    public static double bonusFor(Race race, ItemStack weapon)
    {
        boolean matches = switch (race)
        {
            case DWARF, TROLL -> weapon.getItem() instanceof AxeItem;
            case SERAPH, DEMON, REVENANT -> weapon.getItem() instanceof SwordItem;
            case MERFOLK -> weapon.getItem() instanceof TridentItem;
            // Beastkin fights barehanded, not with a weapon type - matches "animal-blooded" better than
            // handing them a preferred tool.
            case BEASTKIN -> weapon.isEmpty();
            default -> false;
        };

        return matches ? BONUS : 0;
    }

    /**
     * Bonus damage multiplier for this race, specifically for a bolt fired from a crossbow rather than a bow -
     * currently just Fae. Distinct from Elf's own bow/crossbow-blind bonus on RaceStats#bowDamageMultiplier
     * (which applies to any AbstractArrow regardless of what fired it), this one only applies when the arrow's
     * own shotFromCrossbow() flag (confirmed via decompile - a real vanilla field on AbstractArrow, set by
     * CrossbowItem at the moment of firing) is true, so an arrow fired from a plain bow gets nothing here.
     */
    public static double crossbowBonusFor(Race race, boolean shotFromCrossbow)
    {
        return race == Race.FAE && shotFromCrossbow ? BONUS : 0;
    }
}
