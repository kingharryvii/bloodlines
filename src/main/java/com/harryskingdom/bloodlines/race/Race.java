package com.harryskingdom.bloodlines.race;

import java.util.Arrays;
import java.util.List;

public enum Race
{
    HUMAN(Tier.COMMON, "Human", "Balanced and lucky — the baseline every other bloodline is measured against.", false, null),
    WOOD_ELF(Tier.COMMON, "Wood Elf", "Forest-born and swift, more at home among trees than towns.", false, null),
    HIGH_ELF(Tier.UNCOMMON, "High Elf", "Old magical nobility, sharper of mind than of blade.", false, null),
    MOON_ELF(Tier.UNCOMMON, "Moon Elf", "Night-blooded wanderers, most at home under a dark sky.", false, null),
    DWARF(Tier.COMMON, "Dwarf", "Masters of stone and tunnel, born knowing the weight of the earth.", false, null),
    ORC(Tier.COMMON, "Orc", "Brute strength and endurance, favoring the axe over the book.", false, null),
    FAE(Tier.UNCOMMON, "Fae", "Small and touched by old magic, with delicate wings built for true flight — though even Fae wings tire.", false, null),
    FELINE(Tier.UNCOMMON, "Feline", "Swift hunters who never fear the fall.", false, null),
    GOBLIN(Tier.UNCOMMON, "Goblin", "Underground tricksters, quick of hand and quicker to flee.", false, null),

    DRAGONBORN(Tier.RARE, "Dragonborn", "Blood-kin to dragons, and by extension, to the Crown itself.", true,
            "Discover the Dragon Shrine and awaken the blood of House Vaelharys."),
    BEASTKIN(Tier.UNCOMMON, "Beastkin", "Animal-blooded folk whose senses and strength answer to the moon.", false, null),
    REVENANT(Tier.RARE, "Revenant", "A corpse that would not stay in the ground, and hungers for what it lost.", false, null),
    LICH(Tier.RARE, "Lich", "A caster who traded their body for undying power, and paid for it.", false, null),
    GHOUL(Tier.RARE, "Ghoul", "Undead and unfeeling, built to endure what mortals cannot.", false, null),
    DEMON(Tier.RARE, "Demon", "Infernal blood runs hot — fire given a mortal shape.", false, null),
    TROLL(Tier.RARE, "Troll", "Ancient giants who shrug off wounds that would kill anyone else.", false, null),
    MERFOLK(Tier.RARE, "Merfolk", "Sea-born people, as deadly in the depths as they are graceful.", false, null),
    SERAPH(Tier.RARE, "Seraph", "Winged and towering, radiant with a light that isn't always kind.", false, null);

    private final Tier tier;
    private final String displayName;
    private final String description;
    private final boolean locked;
    private final String unlockHint;

    Race(Tier tier, String displayName, String description, boolean locked, String unlockHint)
    {
        this.tier = tier;
        this.displayName = displayName;
        this.description = description;
        this.locked = locked;
        this.unlockHint = unlockHint;
    }

    public Tier getTier()
    {
        return tier;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDescription()
    {
        return description;
    }

    /** True if this race must be unlocked in-game before a player can select it. */
    public boolean isLocked()
    {
        return locked;
    }

    /** Flavor text describing how to unlock this race. Null for races available from the start. */
    public String getUnlockHint()
    {
        return unlockHint;
    }

    /** Races every new player can choose from immediately, no unlock required. */
    public static List<Race> startingRaces()
    {
        return Arrays.stream(values()).filter(race -> !race.locked).toList();
    }

    /** Races that must be unlocked through gameplay before they appear as a choice. */
    public static List<Race> unlockableRaces()
    {
        return Arrays.stream(values()).filter(race -> race.locked).toList();
    }

    public enum Tier
    {
        COMMON,
        UNCOMMON,
        RARE
    }
}
