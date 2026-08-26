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

    static
    {
        BUILDER.comment("Fae flight - hunger gating and speed (see RaceFlightFood/RaceEffects).").push("fae_flight");

        FAE_REQUIRED_FOOD_LEVEL = BUILDER
                .comment("Minimum hunger (0-20) required to fly at all. Falling below this while flying grounds the player until it recovers.")
                .defineInRange("requiredFoodLevel", 7, 0, 20);

        FAE_EXHAUSTION_PER_BOOST_TICK = BUILDER
                .comment("Food exhaustion added per tick while actively flying forward (not just airborne). Vanilla sprinting is 0.1/tick for reference.")
                .defineInRange("exhaustionPerBoostTick", 0.03, 0.0, 1.0);

        FAE_FLYING_SPEED = BUILDER
                .comment("Flying speed, same units as vanilla creative flight speed (vanilla default is 0.05).")
                .defineInRange("flyingSpeed", 0.042, 0.005, 0.5);

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
                .defineInRange("cooldownSeconds", 45, 1, 3600);

        ABILITY_DURATION_SECONDS = BUILDER
                .comment("Seconds the ability's effects last once activated.")
                .defineInRange("durationSeconds", 10, 1, 600);

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
    }
}
