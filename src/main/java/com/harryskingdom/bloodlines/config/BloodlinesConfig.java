package com.harryskingdom.bloodlines.config;

import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * SERVER-type config (not COMMON) - these values gate real gameplay (ability cooldowns, flight food drain, the
 * per-race armor restrictions) and are read on the client too (HUD cooldown bars, tooltips), so they need to actually match
 * between server and client rather than each side trusting its own local file. Forge syncs SERVER configs to
 * connecting clients automatically for exactly this reason - a plain COMMON config would let a client silently
 * drift from what the server enforces (e.g. HUD showing a 45s cooldown bar while the server actually enforces the
 * admin's real 30s). The trade-off: like any SERVER config, values only exist while a world is loaded (see
 * SPEC.isLoaded() checks in BloodlinesConfigScreen) - there's nothing to sync before you've joined one.
 */
public final class BloodlinesConfig
{
    // Shared with BloodlinesConfigScreen (client-side clamping before an edit is even sent) and
    // UpdateBloodlinesConfigPacket (server-side clamping of whatever a client actually sent) - one source of
    // truth for the valid range of each value, instead of the same numbers copy-pasted in three places.
    public static final int FAE_FOOD_LEVEL_MIN = 0, FAE_FOOD_LEVEL_MAX = 20;
    public static final double FAE_EXHAUSTION_MIN = 0.0, FAE_EXHAUSTION_MAX = 1.0;
    public static final double FAE_FLY_SPEED_MIN = 0.005, FAE_FLY_SPEED_MAX = 0.5;
    public static final int ABILITY_SECONDS_MIN = 1, ABILITY_COOLDOWN_MAX = 3600, ABILITY_DURATION_MAX = 600;
    public static final double ORB_RARITY_MULTIPLIER_MIN = 0.0, ORB_RARITY_MULTIPLIER_MAX = 10.0;

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue FAE_REQUIRED_FOOD_LEVEL;
    public static final ForgeConfigSpec.DoubleValue FAE_EXHAUSTION_PER_BOOST_TICK;
    public static final ForgeConfigSpec.DoubleValue FAE_FLYING_SPEED;
    public static final ForgeConfigSpec.EnumValue<MaxArmorTier> FAE_MAX_ARMOR_TIER;
    public static final ForgeConfigSpec.EnumValue<MaxArmorTier> ANGELKIN_MAX_ARMOR_TIER;
    public static final ForgeConfigSpec.EnumValue<MaxArmorTier> DEMONKIN_MAX_ARMOR_TIER;
    public static final ForgeConfigSpec.IntValue ABILITY_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue ABILITY_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue ORB_SPAWN_RARITY_MULTIPLIER;

    static
    {
        BUILDER.comment("Fae flight - hunger gating and speed (see RaceFlightFood/RaceEffects).").push("fae_flight");

        FAE_REQUIRED_FOOD_LEVEL = BUILDER
                .comment("Minimum hunger (0-20) required to fly at all. Falling below this while flying grounds the player until it recovers.")
                .defineInRange("requiredFoodLevel", 7, FAE_FOOD_LEVEL_MIN, FAE_FOOD_LEVEL_MAX);

        FAE_EXHAUSTION_PER_BOOST_TICK = BUILDER
                .comment("Food exhaustion added per tick while actively flying forward (not just airborne). Vanilla sprinting is 0.1/tick for reference.")
                .defineInRange("exhaustionPerBoostTick", 0.03, FAE_EXHAUSTION_MIN, FAE_EXHAUSTION_MAX);

        FAE_FLYING_SPEED = BUILDER
                .comment("Flying speed, same units as vanilla creative flight speed (vanilla default is 0.05).")
                .defineInRange("flyingSpeed", 0.042, FAE_FLY_SPEED_MIN, FAE_FLY_SPEED_MAX);

        BUILDER.pop();
        BUILDER.comment("Armor restriction for the flying races - wings need freedom of movement. Set per-race rather than shared, " +
                "since a race's own health/kit might justify a different cap (e.g. Fae's much lower health than Angelkin/Demonkin).").push("flight_armor");

        FAE_MAX_ARMOR_TIER = BUILDER
                .comment("Heaviest armor tier Fae may equip per slot, measured against that tier's own defense value " +
                        "(so modded armor is caught fairly, not just a hardcoded item list). UNRESTRICTED disables the check.")
                .defineEnum("faeMaxArmorTier", MaxArmorTier.CHAIN);

        ANGELKIN_MAX_ARMOR_TIER = BUILDER
                .comment("Heaviest armor tier Angelkin may equip per slot. See faeMaxArmorTier for how the comparison works.")
                .defineEnum("angelkinMaxArmorTier", MaxArmorTier.CHAIN);

        DEMONKIN_MAX_ARMOR_TIER = BUILDER
                .comment("Heaviest armor tier Demonkin may equip per slot. See faeMaxArmorTier for how the comparison works.")
                .defineEnum("demonkinMaxArmorTier", MaxArmorTier.CHAIN);

        BUILDER.pop();
        BUILDER.comment("The one active, cooldown-gated special move every race has (see RaceAbility).").push("ability");

        ABILITY_COOLDOWN_SECONDS = BUILDER
                .comment("Seconds between uses of a race's active ability.")
                .defineInRange("cooldownSeconds", 45, ABILITY_SECONDS_MIN, ABILITY_COOLDOWN_MAX);

        ABILITY_DURATION_SECONDS = BUILDER
                .comment("Seconds the ability's effects last once activated.")
                .defineInRange("durationSeconds", 10, ABILITY_SECONDS_MIN, ABILITY_DURATION_MAX);

        BUILDER.pop();
        BUILDER.comment("Orb of Bloodlines - lets a player reroll their race, found as rare structure loot (see OrbOfOriginLootModifier " +
                "and data/harrys_bloodlines/loot_modifiers/*.json).").push("orb_of_bloodlines");

        ORB_SPAWN_RARITY_MULTIPLIER = BUILDER
                .comment("Scales every structure's own hand-tuned drop chance by this factor (1.0 = unchanged, 2.0 = twice as common, " +
                        "0.5 = half as common) - the relative rarity between structures (harder structures already drop it more often) " +
                        "stays the same, this only turns the overall frequency up or down.")
                .defineInRange("spawnRarityMultiplier", 1.0, ORB_RARITY_MULTIPLIER_MIN, ORB_RARITY_MULTIPLIER_MAX);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private BloodlinesConfig() {}

    public enum MaxArmorTier
    {
        UNRESTRICTED(null, "unrestricted"),
        LEATHER(ArmorMaterials.LEATHER, "leather"),
        GOLD(ArmorMaterials.GOLD, "gold"),
        CHAIN(ArmorMaterials.CHAIN, "chainmail"),
        IRON(ArmorMaterials.IRON, "iron"),
        DIAMOND(ArmorMaterials.DIAMOND, "diamond"),
        NETHERITE(ArmorMaterials.NETHERITE, "netherite");

        private final ArmorMaterial material;
        // The enum constant name is what's shown on the config screen's cycle button (see BloodlinesConfigScreen)
        // - fine there since it's next to a labelled row, but "chain" reads oddly in a sentence, so the in-game
        // rejection message (RaceEffectEvents) uses this instead.
        private final String displayName;

        MaxArmorTier(ArmorMaterial material, String displayName)
        {
            this.material = material;
            this.displayName = displayName;
        }

        public ArmorMaterial material()
        {
            return material;
        }

        public String displayName()
        {
            return displayName;
        }

        /** Never throws - a value from over the network shouldn't be able to crash the handler reading it. */
        public static java.util.Optional<MaxArmorTier> tryParse(String name)
        {
            try
            {
                return java.util.Optional.of(MaxArmorTier.valueOf(name));
            }
            catch (IllegalArgumentException e)
            {
                return java.util.Optional.empty();
            }
        }
    }
}
