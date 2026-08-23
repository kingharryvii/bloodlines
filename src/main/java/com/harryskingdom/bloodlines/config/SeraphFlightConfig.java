package com.harryskingdom.bloodlines.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Tunable values for Seraph's native flight (see SeraphFlightController/SeraphWingsLayer). Common config so both
 * the client (local physics prediction) and server (food-cost/cooldown validation) read the same numbers.
 */
public final class SeraphFlightConfig
{
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue FLAP_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue FLAP_UPWARD_FORCE;
    public static final ForgeConfigSpec.DoubleValue FLAP_FORWARD_FORCE;
    public static final ForgeConfigSpec.DoubleValue TAKEOFF_UPWARD_FORCE;
    public static final ForgeConfigSpec.DoubleValue TAKEOFF_FORWARD_FORCE;
    public static final ForgeConfigSpec.DoubleValue MAX_FLIGHT_SPEED;
    public static final ForgeConfigSpec.DoubleValue MAX_ASCENT_SPEED;
    public static final ForgeConfigSpec.DoubleValue FLIGHT_GRAVITY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LOOK_STEER_TARGET_SPEED;
    public static final ForgeConfigSpec.DoubleValue LOOK_STEER_BLEND_RATE;
    public static final ForgeConfigSpec.DoubleValue LOOK_STEER_GLIDE_BLEND_RATE;
    public static final ForgeConfigSpec.DoubleValue CLIMB_BOOST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CLIMB_BOOST_ANGLE_DEGREES;
    public static final ForgeConfigSpec.DoubleValue PASSIVE_EXHAUSTION_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue FLAP_EXHAUSTION_COST;
    public static final ForgeConfigSpec.IntValue REQUIRED_FOOD_LEVEL;
    public static final ForgeConfigSpec.BooleanValue ARMOR_AFFECTS_FLIGHT_SPEED;
    public static final ForgeConfigSpec.BooleanValue INDEFINITE_FLIGHT;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Seraph native flight tuning").push("seraph_flight");

        FLAP_COOLDOWN_TICKS = builder
                .comment("Ticks that must pass between wing flaps (20 ticks = 1 second).")
                .defineInRange("flapCooldownTicks", 12, 1, 200);

        FLAP_UPWARD_FORCE = builder
                .comment("Upward velocity added by a single flap while airborne.")
                .defineInRange("flapUpwardForce", 0.6, 0.0, 5.0);

        FLAP_FORWARD_FORCE = builder
                .comment("Forward velocity (along look direction) added by a single flap while airborne.")
                .defineInRange("flapForwardForce", 0.35, 0.0, 5.0);

        TAKEOFF_UPWARD_FORCE = builder
                .comment("Upward velocity added by the initial takeoff flap from the ground.")
                .defineInRange("takeoffUpwardForce", 0.75, 0.0, 5.0);

        TAKEOFF_FORWARD_FORCE = builder
                .comment("Forward velocity added by the initial takeoff flap from the ground.")
                .defineInRange("takeoffForwardForce", 0.2, 0.0, 5.0);

        MAX_FLIGHT_SPEED = builder
                .comment("Horizontal speed cap (blocks/tick) while flying.")
                .defineInRange("maxFlightSpeed", 1.2, 0.1, 10.0);

        MAX_ASCENT_SPEED = builder
                .comment("Vertical ascent speed cap (blocks/tick) while flying.")
                .defineInRange("maxAscentSpeed", 0.5, 0.1, 5.0);

        FLIGHT_GRAVITY_MULTIPLIER = builder
                .comment("Fraction of vanilla's own fall-flying gravity that still applies while airborne as a " +
                        "Seraph (0 = float, 1 = full elytra-glide gravity). Lift from flaps counters the rest.")
                .defineInRange("flightGravityMultiplier", 0.35, 0.0, 1.0);

        LOOK_STEER_TARGET_SPEED = builder
                .comment("Terminal speed (blocks/tick) velocity chases while thrusting, in whatever direction the " +
                        "player is looking - the core of Icarus-style look-directed flight (player.getLookAngle() " +
                        "scaled up, velocity eased toward it every tick).")
                .defineInRange("lookSteerTargetSpeed", 0.9, 0.1, 5.0);

        LOOK_STEER_BLEND_RATE = builder
                .comment("Fraction of the way velocity closes toward lookSteerTargetSpeed each tick while holding " +
                        "forward (thrusting). Icarus's own default is 0.0125, but that number only makes sense " +
                        "alongside vanilla's separate elytra drag, which this standalone system doesn't have.")
                .defineInRange("lookSteerBlendRate", 0.06, 0.001, 1.0);

        LOOK_STEER_GLIDE_BLEND_RATE = builder
                .comment("Same as lookSteerBlendRate but applied while NOT holding forward (gliding) - weaker, so " +
                        "the player keeps some air control coasting on momentum without full thrust.")
                .defineInRange("lookSteerGlideBlendRate", 0.02, 0.0, 1.0);

        CLIMB_BOOST_MULTIPLIER = builder
                .comment("Multiplies the steering blend rate when looking within climbBoostAngleDegrees of " +
                        "straight up - Icarus's own 'power climb' when you look nearly vertical.")
                .defineInRange("climbBoostMultiplier", 1.6, 1.0, 10.0);

        CLIMB_BOOST_ANGLE_DEGREES = builder
                .comment("How close to straight up (degrees) the player must be looking for the climb boost to apply.")
                .defineInRange("climbBoostAngleDegrees", 15.0, 1.0, 90.0);

        PASSIVE_EXHAUSTION_PER_TICK = builder
                .comment("Food exhaustion added per tick just for staying airborne, regardless of flapping. " +
                        "Matches Medieval Origins Revival's own Valkyrie race (medievalorigins:icarus_wings power, " +
                        "which grants real Icarus wings with this exact exhaustion value) rather than Icarus's " +
                        "own default of 0.03 - Valkyrie is tuned about 4x more forgiving.")
                .defineInRange("passiveExhaustionPerTick", 0.0075, 0.0, 1.0);

        FLAP_EXHAUSTION_COST = builder
                .comment("Additional food exhaustion added each time the player flaps. No Icarus/Valkyrie " +
                        "equivalent (manual flapping is Bloodlines' own addition), kept low since flapping is a " +
                        "burst rather than a requirement.")
                .defineInRange("flapExhaustionCost", 0.15, 0.0, 5.0);

        REQUIRED_FOOD_LEVEL = builder
                .comment("Minimum food level required to take off or keep flapping. Valkyrie's own tuning is 0 " +
                        "(no gate at all, just the passive drain) - kept at 1 here so literal starvation still " +
                        "grounds the player, matching 'insufficient food eventually prevents flight'.")
                .defineInRange("requiredFoodLevel", 1, 0, 20);

        ARMOR_AFFECTS_FLIGHT_SPEED = builder
                .comment("If true, heavier armor slows flight speed (not yet wired to a specific formula - reserved for tuning).")
                .define("armorAffectsFlightSpeed", false);

        INDEFINITE_FLIGHT = builder
                .comment("If true, Seraph can fly indefinitely regardless of food level (skips all food gating/exhaustion).")
                .define("indefiniteFlight", false);

        builder.pop();

        SPEC = builder.build();
    }

    private SeraphFlightConfig() {}
}
