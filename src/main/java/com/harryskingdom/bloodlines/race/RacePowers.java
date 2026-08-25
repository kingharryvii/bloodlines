package com.harryskingdom.bloodlines.race;

import java.util.List;
import java.util.Map;

import static com.harryskingdom.bloodlines.race.RacePower.Category.NEGATIVE;
import static com.harryskingdom.bloodlines.race.RacePower.Category.NEUTRAL;
import static com.harryskingdom.bloodlines.race.RacePower.Category.POSITIVE;

/**
 * The power list shown on each race's selection card (see BloodlineSelectScreen) - one entry per meaningful
 * RaceStats field or RaceAbility, narrated for the player. Kept in sync by hand with RaceStats/RaceAbility's
 * actual numbers; there's no runtime link between them since RaceStats is plain numeric bonuses, not a
 * self-describing power system.
 */
public final class RacePowers
{
    private static final Map<Race, List<RacePower>> POWERS = Map.ofEntries(
            Map.entry(Race.HUMAN, List.of(
                    new RacePower("Adaptable", "Small bonuses to health, melee and ranged damage - no specialization, no weakness.", POSITIVE),
                    new RacePower("Quick Study", "+10% attack speed and +10% mining speed.", POSITIVE),
                    new RacePower("Fortune's Favor", "Slightly luckier than most, with a natural aptitude for magic.", POSITIVE),
                    new RacePower("Second Wind", "Ability: regeneration and resistance on demand.", NEUTRAL)
            )),
            Map.entry(Race.WOOD_ELF, List.of(
                    new RacePower("Swift Feet", "+10% movement speed.", POSITIVE),
                    new RacePower("Deadly Aim", "+25% bow damage - the sharpest shot of any bloodline.", POSITIVE),
                    new RacePower("Thin Blood", "-2 max health.", NEGATIVE),
                    new RacePower("Hunter's Mark", "Ability: bursts of speed and jump height.", NEUTRAL)
            )),
            Map.entry(Race.HIGH_ELF, List.of(
                    new RacePower("Arcane Nobility", "+2 luck.", POSITIVE),
                    new RacePower("Sharpened Mind", "+10% attack speed.", POSITIVE),
                    new RacePower("Frail Arm", "-1 melee damage.", NEGATIVE),
                    new RacePower("Arcane Surge", "Ability: absorption shielding and a burst of luck.", NEUTRAL)
            )),
            Map.entry(Race.MOON_ELF, List.of(
                    new RacePower("Night Eyes", "Full night vision at all times.", NEUTRAL),
                    new RacePower("Silent Step", "+5% movement speed.", POSITIVE),
                    new RacePower("Pale Blood", "-2 max health.", NEGATIVE),
                    new RacePower("Umbral Step", "Ability: brief invisibility.", NEUTRAL)
            )),
            Map.entry(Race.DWARF, List.of(
                    new RacePower("Stout Frame", "+2 max health and knockback resistance.", POSITIVE),
                    new RacePower("Forge-Born", "Immune to fire and lava.", NEUTRAL),
                    new RacePower("Tunnel Sense", "+30% mining speed - unmatched underground.", POSITIVE),
                    new RacePower("Short Stride", "-5% movement speed.", NEGATIVE),
                    new RacePower("Stoneskin", "Ability: damage resistance.", NEUTRAL)
            )),
            Map.entry(Race.FAE, List.of(
                    new RacePower("True Flight", "Real, sustained flight on delicate wings.", POSITIVE),
                    new RacePower("Featherfall", "Never takes fall damage - drifts down gently instead.", NEUTRAL),
                    new RacePower("Lucky Dust", "+2 luck.", POSITIVE),
                    new RacePower("Hollow Bones", "-6 max health, -1 melee damage - the frailest bloodline.", NEGATIVE),
                    new RacePower("Nature's Blessing", "Ability: regeneration for you and nearby allies.", NEUTRAL)
            )),
            Map.entry(Race.GOBLIN, List.of(
                    new RacePower("Lucky Scavenger", "+2 luck.", POSITIVE),
                    new RacePower("Tunnel Rat", "+30% mining speed.", POSITIVE),
                    new RacePower("Weak Grip", "-1 melee damage, -2 max health.", NEGATIVE),
                    new RacePower("Smoke Bomb", "Ability: invisibility and a burst of speed to escape.", NEUTRAL)
            )),
            Map.entry(Race.BEASTKIN, List.of(
                    new RacePower("Feral Strength", "+2 melee damage, +2 max health.", POSITIVE),
                    new RacePower("Predator's Grace", "+10% movement speed, +10% attack speed, and a knack for jumping.", POSITIVE),
                    new RacePower("Sure-Footed", "Takes 50% less fall damage, and knockback resistance.", POSITIVE),
                    new RacePower("Moonlit Eyes", "Full night vision.", NEUTRAL),
                    new RacePower("Clumsy Hands", "-10% mining speed.", NEGATIVE),
                    new RacePower("Feral Howl", "Ability: damage, speed and resistance.", NEUTRAL)
            )),
            Map.entry(Race.REVENANT, List.of(
                    new RacePower("Hungers For More", "Heals for 25% of all damage dealt.", POSITIVE),
                    new RacePower("Grave-Quick", "+10% movement speed, +15% attack speed.", POSITIVE),
                    new RacePower("Kindred of the Grave", "Hostile undead mobs won't target you.", NEUTRAL),
                    new RacePower("Wasting Flesh", "-2 max health.", NEGATIVE),
                    new RacePower("Siphon", "Ability: a damage boost and an instant heal.", NEUTRAL)
            )),
            Map.entry(Race.GHOUL, List.of(
                    new RacePower("Undying Endurance", "+4 max health, never takes fall damage.", POSITIVE),
                    new RacePower("Iron Grip", "+1 melee damage and knockback resistance.", POSITIVE),
                    new RacePower("Grave Sight", "Full night vision; hostile undead mobs won't target you.", NEUTRAL),
                    new RacePower("Shambling Gait", "-10% movement speed.", NEGATIVE),
                    new RacePower("Cursed Luck", "-1 luck.", NEGATIVE),
                    new RacePower("Undying Resolve", "Ability: damage resistance and regeneration.", NEUTRAL)
            )),
            Map.entry(Race.DEMON, List.of(
                    new RacePower("Winged", "Real, sustained flight.", POSITIVE),
                    new RacePower("Hellborne", "Fully immune to fire and lava.", NEUTRAL),
                    new RacePower("Infernal Might", "+2 melee damage, +10% attack speed, +2 max health.", POSITIVE),
                    new RacePower("Nightsight", "Full night vision.", NEUTRAL),
                    new RacePower("Sure Wings", "Takes 60% less fall damage.", POSITIVE),
                    new RacePower("Need for Mobility", "Can't wear armor heavier than chainmail.", NEGATIVE),
                    new RacePower("Infernal Wrath", "Ability: a damage boost for you and nearby allies, plus a burst of speed for you.", NEUTRAL)
            )),
            Map.entry(Race.TROLL, List.of(
                    new RacePower("Ancient Hide", "+8 max health - the toughest of any bloodline.", POSITIVE),
                    new RacePower("Crushing Blows", "+3 melee damage and strong knockback resistance.", POSITIVE),
                    new RacePower("Stone Fists", "+20% mining speed.", POSITIVE),
                    new RacePower("Lumbering", "-15% movement speed, -15% attack speed.", NEGATIVE),
                    new RacePower("Regenerate", "Ability: regeneration and damage resistance.", NEUTRAL)
            )),
            Map.entry(Race.MERFOLK, List.of(
                    new RacePower("Born of the Tide", "Breathes underwater and swims freely - at home in the depths.", POSITIVE),
                    new RacePower("Sea's Fortune", "+1 luck.", POSITIVE),
                    new RacePower("Depths' Strength", "Knockback resistance, even on land.", POSITIVE),
                    new RacePower("Out of Your Depth", "-5% movement speed on land.", NEGATIVE),
                    new RacePower("Tidal Surge", "Ability: a burst of speed and jump height.", NEUTRAL)
            )),
            Map.entry(Race.SERAPH, List.of(
                    new RacePower("Angel Wings", "Real, sustained flight on white feathered wings.", POSITIVE),
                    new RacePower("Protective Wings", "Takes 60% less fall damage.", POSITIVE),
                    new RacePower("Holy Vigor", "+4 max health, +2 melee damage, +2 luck.", POSITIVE),
                    new RacePower("Heavy of Wing", "-10% movement speed, -20% mining speed.", NEGATIVE),
                    new RacePower("Need for Mobility", "Can't wear armor heavier than chainmail.", NEGATIVE),
                    new RacePower("Divine Descent", "Ability: heals you and nearby allies, plus a jump, damage boost and shield for you.", NEUTRAL)
            )),
            Map.entry(Race.DRAGONBORN, List.of(
                    new RacePower("Sealed Blood", "Locked until the Dragon Shrine is discovered and its power awakened.", NEUTRAL)
            ))
    );

    private RacePowers() {}

    public static List<RacePower> of(Race race)
    {
        return POWERS.getOrDefault(race, List.of());
    }
}
