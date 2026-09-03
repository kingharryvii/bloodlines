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
            // healthBonus -4 (was -6): still by far the frailest race in the game (next-worst penalty anywhere
            // is -2), but softened now that flight costs three separate things instead of one - hunger-gating
            // and the flying-race armor cap (both added this session) already tax the same "flight" perk the
            // -6 was originally sized to cover alone.
            case FAE -> new RaceStats(-4, 0.15, -1, 0, 2, 0, false, false, true, false, 0, 0, false, 0, false, 0);
            // speedMultiplier +0.1 and attackSpeedMultiplier +0.1 added: "underground tricksters, quick of hand
            // and quicker to flee" had nothing backing "quick" outside the active ability's own temporary burst -
            // the passive kit was identical in speed profile to a race with no speed theme at all. Matches Wood
            // Elf's own "swift" baseline (+0.1 speed) rather than inventing a new number.
            case GOBLIN -> new RaceStats(-2, 0.1, -1, 0, 2, 0.1, false, false, false, false, 0.3, 0, false, 0, false, 0);

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
            // luckBonus -1 added (matches Ghoul/Demonkin's own "cursed undead" penalty) and attackSpeedMultiplier
            // brought down from 0.15 to 0.1 - was the single highest attack speed of any race, stacked on top of
            // being the only race with passive lifesteal (25%, everyone else's sustain is ability-gated only).
            // That combination read as stronger in practice than the stat sheet alone suggested, so the attack
            // speed piece comes down to the same 0.1 ceiling Human/High Elf/Demonkin/Beastkin already share,
            // while the passive lifesteal itself - the actually distinctive part of "vampiric hunger" - stays
            // untouched.
            case REVENANT -> new RaceStats(-2, 0.1, 2, 0, -1, 0.1, true, false, false, false, 0, 0, false, 0.25, true, 0);
            case GHOUL -> new RaceStats(4, -0.1, 1, 0.15, -1, 0, true, false, false, true, 0, 0, false, 0, true, 0);
            // healthBonus +3 (was -2, then +2): offsets the chainmail-only armor cap added alongside flight -
            // without it, Demonkin stacked "squishiest race" and "capped gear" with no defensive compensation.
            // Now matches Angelkin's own health exactly (see SERAPH below) - the two winged RARE races otherwise
            // trade an aggressive kit (Demonkin: positive speed, an attack speed bonus, night vision, fire
            // resistance) for a defensive/support one (Angelkin: knockback resistance, luck, party healing), so
            // there was no longer a good reason for one to also just have more health than the other outright.
            // fallDamageReductionPercent 0.6 (not noFallDamage/slowFalling): flying
            // doesn't mean immune to a hard landing - 60% reduction feels like a flier who's still capable of
            // misjudging a fall, not a race with actual slow-fall magic. luckBonus -1 and miningSpeedMultiplier
            // -0.1 added: before this, Demonkin was the only race in the game with zero negative stats at all -
            // every value neutral or positive, no drawback besides the armor cap shared with Fae/Angelkin. -1
            // luck matches Ghoul's own "cursed" penalty (an infernal race being unlucky needs no more than that
            // to sell "it's a demon"), -10% mining matches Angelkin/Beastkin so all three "less grounded" races
            // share one consistent number instead of each inventing their own.
            case DEMON -> new RaceStats(3, 0.05, 2, 0, -1, 0.1, true, true, false, false, -0.1, 0, false, 0, false, 0.6);
            // healthBonus +7 (was +8) and attackDamageBonus +2 (was +3): Troll was the only race with the single
            // highest stat in three separate categories at once (health, damage, AND knockback resistance) -
            // damage brought down to the same +2 ceiling every other RARE combat race already sits at (Beastkin/
            // Demonkin/Angelkin), plus a smaller health trim on top. Speed/attack speed (-0.15 each) deliberately
            // left untouched - already the steepest combined mobility penalty of any race, nobody else goes
            // below -0.15 on either stat, so cutting further risked overcorrecting into "not fun" rather than
            // "appropriately less dominant". Health and knockback resistance staying otherwise strong keeps
            // "ancient giant who shrugs off wounds that would kill anyone else" fully intact as the core fantasy.
            case TROLL -> new RaceStats(7, -0.15, 2, 0.2, 0, -0.15, false, false, false, false, 0.2, 0, false, 0, false, 0);
            // speedMultiplier -0.05 (was -0.1) and +knockbackResistance: still worse on land than a Merfolk in
            // water, but not just a flat penalty with nothing to show for it out of the sea - a "strength from
            // the depths" nod that matters regardless of whether you're actually swimming. attackDamageBonus +1
            // and attackSpeedMultiplier +0.1 added: "as deadly in the depths as they are graceful" had nothing
            // behind "deadly" - zero offensive stat of any kind, the only RARE race with no combat-relevant stat
            // at all (the ability, Tidal Surge, is pure mobility too). Kept modest rather than matching a
            // dedicated bruiser's numbers, since the theme leans graceful/precise, not brute strength.
            case MERFOLK -> new RaceStats(0, -0.05, 1, 0.1, 1, 0.1, false, false, false, false, 0, 0, true, 0, false, 0);
            // fallDamageReductionPercent 0.6, not noFallDamage/slowFalling: same reasoning as Demonkin above -
            // flight doesn't grant true fall immunity, just makes a bad landing hurt less. fireResistant=false:
            // an angel isn't a fire elemental - that belongs to Demonkin, not Angelkin. healthBonus +3 (was +6,
            // then +4): brought down to match Demonkin's own +3 exactly - the two winged RARE races otherwise
            // trade an aggressive kit for a defensive/support one (see the DEMON comment above), and there was no
            // longer a good reason for Angelkin to also just have more raw health than Demonkin on top of that.
            case SERAPH -> new RaceStats(3, -0.1, 2, 0.15, 2, 0, false, false, false, false, -0.2, 0, false, 0, false, 0.6);

            // Dragonborn has no stats yet — designed alongside the Dragon Shrine unlock.
            default -> NONE;
        };
    }
}
