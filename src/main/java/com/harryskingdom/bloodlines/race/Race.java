package com.harryskingdom.bloodlines.race;

import java.util.Arrays;
import java.util.List;

public enum Race
{
    HUMAN(Tier.COMMON, "Demi-Human", "Balanced and lucky — the baseline every other bloodline is measured against.", false, null),
    DWARF(Tier.COMMON, "Dwarf", "Masters of stone and tunnel, born knowing the weight of the earth.", false, null),
    FAE(Tier.UNCOMMON, "Fae", "Small and touched by old magic, with delicate wings built for true flight — though even Fae wings tire.", false, null),
    // Display name only ("Shadowkin" instead of "Goblin") - same internal-vs-display split already used
    // for TROLL/Ogre and SERAPH/Angelkin, so no save-data migration is needed for something that's purely
    // cosmetic. Renamed to stop clashing with the actual goblin NPC traders another mod adds to the world -
    // the kit (Smoke Bomb, +luck/+speed/+mining, no muscle) already reads as "shadowy trickster" more than
    // "goblin" specifically, so the name change costs nothing thematically.
    GOBLIN(Tier.UNCOMMON, "Shadowkin", "Creatures of shadow and stealth, quick of hand and quicker to vanish.", false, null),

    DRAGONBORN(Tier.RARE, "Dragonborn", "Blood-kin to dragons, and by extension, to the Crown itself.", true,
            "Discover the Dragon Shrine and awaken the blood of House Vaelharys."),
    // Absorbed the old Feline race (agility, claws, never fearing the fall) - the two kits overlapped enough
    // that keeping them separate was redundant rather than distinct.
    BEASTKIN(Tier.RARE, "Beastkin", "Animal-blooded folk, swift and strong, whose senses and instincts answer to the moon.", false, null),
    // Absorbed the old Wood Elf, High Elf, and Moon Elf races - three thin, low-key COMMON/UNCOMMON kits with
    // real overlap (all three leaned "swift", none had much identity beyond one headline stat) merged into one
    // fuller RARE race instead, the same "combine and bump tier" call already made for Beastkin/Feline. See
    // PlayerRaceProvider#tryParseRace for the save-data migration - anyone who had one of the three old races
    // picked lands on this one automatically, not back at race-selection.
    ELF(Tier.RARE, "Elf", "Old magical bloodlines of forest and moonlight, keener of eye and quicker of hand than most.", false, null),
    REVENANT(Tier.RARE, "Revenant", "A corpse that would not stay in the ground, and hungers for what it lost.", false, null),
    DEMON(Tier.RARE, "Demonkin", "Infernal blood runs hot — fire given a mortal shape.", false, null),
    // Display name only ("Ogre" instead of "Troll") - same internal-vs-display split already used for
    // SERAPH/Angelkin, so no save-data migration is needed for something that's purely cosmetic.
    TROLL(Tier.RARE, "Ogre", "Ancient giants who shrug off wounds that would kill anyone else.", false, null),
    MERFOLK(Tier.RARE, "Merfolk", "Sea-born people, as deadly in the depths as they are graceful.", false, null),
    SERAPH(Tier.RARE, "Angelkin", "Winged and radiant, with a light that isn't always kind.", false, null);

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
