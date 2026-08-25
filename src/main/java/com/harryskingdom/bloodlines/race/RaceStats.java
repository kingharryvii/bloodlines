package com.harryskingdom.bloodlines.race;

public record RaceStats(
        double healthBonus,
        double speedMultiplier,
        double attackDamageBonus,
        double knockbackResistance,
        double luckBonus,
        double attackSpeedMultiplier,
        boolean nightVision,
        boolean fireResistant,
        boolean slowFalling,
        boolean noFallDamage,
        double miningSpeedMultiplier,
        double bowDamageMultiplier,
        boolean aquatic,
        double lifestealPercent,
        boolean undead,
        double fallDamageReductionPercent
)
{
    private static final RaceStats NONE = new RaceStats(0, 0, 0, 0, 0, 0, false, false, false, false, 0, 0, false, 0, false, 0);

    public static RaceStats of(Race race)
    {
        return switch (race)
        {
            // Jack-of-all-trades: small nudges spread across melee, ranged, luck, speed and mining rather than
            // one specialization - each individual number is well below any specialist race's number in that
            // same category (e.g. bowDamageMultiplier 0.05 vs Wood Elf's 0.25), so Human is versatile without
            // matching a dedicated race at its own game. attackSpeedMultiplier/luckBonus double as the "can
            // dabble in magic too" nod, on the same stats High Elf's own caster kit leans on, just smaller.
            case HUMAN -> new RaceStats(1, 0, 1, 0, 1, 0.1, false, false, false, false, 0.1, 0.05, false, 0, false, 0);
            case WOOD_ELF -> new RaceStats(-2, 0.1, 0, 0, 0, 0, false, false, false, false, 0, 0.25, false, 0, false, 0);
            case HIGH_ELF -> new RaceStats(0, 0, -1, 0, 2, 0.1, false, false, false, false, 0, 0, false, 0, false, 0);
            case MOON_ELF -> new RaceStats(-2, 0.05, 0, 0, 0, 0, true, false, false, false, 0, 0, false, 0, false, 0);
            case DWARF -> new RaceStats(2, -0.05, 0, 0.1, 0, 0, false, true, false, false, 0.3, 0, false, 0, false, 0);
            case FAE -> new RaceStats(-6, 0.15, -1, 0, 2, 0, false, false, true, false, 0, 0, false, 0, false, 0);
            case GOBLIN -> new RaceStats(-2, 0, -1, 0, 2, 0, false, false, false, false, 0.3, 0, false, 0, false, 0);

            // Merged with the old Feline race: Beastkin's own bulk (health, damage, knockback resist, night
            // vision) plus Feline's agility (attack speed) - each number pulled DOWN from a simple sum of the
            // two originals (e.g. +2 damage here, not Feline's +1 plus Beastkin's +3 = +4), since stacking both
            // kits in full would make this the single strongest combat race outright rather than a distinct
            // agile-predator niche next to Troll's own lumbering-tank one. fallDamageReductionPercent 0.5, not
            // noFallDamage: a cat-like knack for landing on their feet, not true immunity - trimmed down from
            // Feline's old full immunity, since a Rare-tier race with no real weakness besides mining stopped
            // reading as "different playstyle" and started reading as "strictly better". Innate low-level Jump
            // Boost applied separately in RaceEffects (a sure-footed, climbing-adjacent nod - there's no clean
            // "can climb walls" stat to add here). Bumped to Rare given how much it carries relative to a
            // typical Uncommon kit.
            case BEASTKIN -> new RaceStats(2, 0.1, 2, 0.1, 0, 0.1, true, false, false, false, -0.1, 0, false, 0, false, 0.5);
            case REVENANT -> new RaceStats(-2, 0.1, 2, 0, 0, 0.15, true, false, false, false, 0, 0, false, 0.25, true, 0);
            case GHOUL -> new RaceStats(4, -0.1, 1, 0.15, -1, 0, true, false, false, true, 0, 0, false, 0, true, 0);
            // healthBonus +3 (was -2, then +2): offsets the chainmail-only armor cap added alongside flight -
            // without it, Demonkin stacked "squishiest race" and "capped gear" with no defensive compensation,
            // unlike Angelkin's own +4 health cushioning the same armor restriction. Still a full point under
            // Angelkin's own +4 despite otherwise having the more aggressive kit between the two (positive speed
            // vs Angelkin's -0.1, an attack speed bonus Angelkin lacks, plus night vision and fire resistance) -
            // see the balance note in RaceAbility/DURATION_TICKS discussion for why that gap is worth watching
            // rather than closing further. fallDamageReductionPercent 0.6 (not noFallDamage/slowFalling): flying
            // doesn't mean immune to a hard landing - 60% reduction feels like a flier who's still capable of
            // misjudging a fall, not a race with actual slow-fall magic.
            case DEMON -> new RaceStats(3, 0.05, 2, 0, 0, 0.1, true, true, false, false, 0, 0, false, 0, false, 0.6);
            case TROLL -> new RaceStats(8, -0.15, 3, 0.2, 0, -0.15, false, false, false, false, 0.2, 0, false, 0, false, 0);
            // speedMultiplier -0.05 (was -0.1) and +knockbackResistance: still worse on land than a Merfolk in
            // water, but not just a flat penalty with nothing to show for it out of the sea - a "strength from
            // the depths" nod that matters regardless of whether you're actually swimming.
            case MERFOLK -> new RaceStats(0, -0.05, 0, 0.1, 1, 0, false, false, false, false, 0, 0, true, 0, false, 0);
            // fallDamageReductionPercent 0.6, not noFallDamage/slowFalling: same reasoning as Demonkin above -
            // flight doesn't grant true fall immunity, just makes a bad landing hurt less. fireResistant=false:
            // an angel isn't a fire elemental - that belongs to Demonkin, not Angelkin. healthBonus +4 (was +6):
            // trimmed to make room for Wing Dive healing nearby allies too, not just self - a passive
            // party-support tool alongside a personal stat bonus would be too much value for one race.
            case SERAPH -> new RaceStats(4, -0.1, 2, 0.15, 2, 0, false, false, false, false, -0.2, 0, false, 0, false, 0.6);

            // Dragonborn has no stats yet — designed alongside the Dragon Shrine unlock.
            default -> NONE;
        };
    }
}
