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
            case HUMAN -> new RaceStats(0, 0, 0, 0, 1, 0.1, false, false, false, false, 0.1, 0, false, 0, false, 0);
            case WOOD_ELF -> new RaceStats(-2, 0.1, 0, 0, 0, 0, false, false, false, false, 0, 0.25, false, 0, false, 0);
            case HIGH_ELF -> new RaceStats(0, 0, -1, 0, 2, 0.1, false, false, false, false, 0, 0, false, 0, false, 0);
            case MOON_ELF -> new RaceStats(-2, 0.05, 0, 0, 0, 0, true, false, false, false, 0, 0, false, 0, false, 0);
            case DWARF -> new RaceStats(2, -0.05, 0, 0.1, 0, 0, false, true, false, false, 0.3, 0, false, 0, false, 0);
            case ORC -> new RaceStats(4, 0, 2, 0.1, 0, 0, false, false, false, false, -0.2, 0, false, 0, false, 0);
            case FAE -> new RaceStats(-6, 0.15, -1, 0, 2, 0, false, false, true, false, 0, 0, false, 0, false, 0);
            case FELINE -> new RaceStats(-2, 0.15, 0, 0, 0, 0.1, false, false, false, true, 0, 0, false, 0, false, 0);
            case GOBLIN -> new RaceStats(-2, 0, -1, 0, 2, 0, false, false, false, false, 0.3, 0, false, 0, false, 0);

            case BEASTKIN -> new RaceStats(4, 0.1, 3, 0.1, 0, -0.1, true, false, false, false, -0.3, 0, false, 0, false, 0);
            case REVENANT -> new RaceStats(-2, 0.1, 2, 0, 0, 0.15, true, false, false, false, 0, 0, false, 0.25, true, 0);
            case LICH -> new RaceStats(-6, -0.05, -2, 0, 3, 0, true, false, false, false, 0, 0, false, 0, true, 0);
            case GHOUL -> new RaceStats(4, -0.1, 1, 0.15, -1, 0, true, false, false, true, 0, 0, false, 0, true, 0);
            case DEMON -> new RaceStats(-2, 0.05, 2, 0, 0, 0.1, true, true, false, false, 0, 0, false, 0, false, 0);
            case TROLL -> new RaceStats(8, -0.15, 3, 0.2, 0, -0.15, false, false, false, false, 0.2, 0, false, 0, false, 0);
            case MERFOLK -> new RaceStats(0, -0.1, 0, 0, 1, 0, false, false, false, false, 0, 0, true, 0, false, 0);
            case SERAPH -> new RaceStats(6, -0.1, 2, 0.15, 2, 0, false, true, false, false, -0.2, 0, false, 0, false, 0.6);

            // Dragonborn has no stats yet — designed alongside the Dragon Shrine unlock.
            default -> NONE;
        };
    }
}
