package com.harryskingdom.bloodlines.client.race;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.config.SeraphFlightConfig;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.SeraphFlapPacket;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.seraph.SeraphFlightState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Seraph's flight controller (Stage 1 of the rebuild - see chat for the staged plan). Runs client-side, local
 * player only, same client-authoritative movement this codebase already uses for vanilla flight and Fae's native
 * flight. Owns physics and state ONLY; SeraphWingsLayer (currently still the ElytraLayer placeholder from the
 * previous pass - the real 4-wing model is Stage 2+) only ever reads state from here, never drives it.
 * <p>
 * The core steering formula is a direct translation of Icarus's own (read from their actual 1.21.1 source,
 * IcarusClient.onPlayerTick - not guessed): each tick, while holding forward, velocity eases toward
 * {@code player.getLookAngle() * targetSpeed} at a configurable rate, with a stronger "power climb" rate when
 * looking within a few degrees of straight up. Icarus's own constants (wingsSpeed=0.0125, target magnitude 2.5)
 * only produce sane numbers because vanilla's own elytra drag is ALSO acting underneath that formula in real
 * Icarus; since this is a fully standalone system with no vanilla fall-flying physics under it, the constants
 * are retuned from scratch (see SeraphFlightConfig) while keeping the same structure - "look up + hold forward
 * to climb, look down to dive, look left/right to steer" falls out of this formula on its own, it isn't a
 * separate special case.
 * <p>
 * Flapping (jump key while airborne) is Bloodlines' own addition on top of that base: a direct upward+forward
 * impulse, cooldown-gated, so repeated controlled flaps let the player climb without diving first - real Icarus
 * has no equivalent, it's pure elytra-glide-plus-steering. Gliding (not holding forward) still gets a much
 * weaker steering nudge, so the player has some air control coasting on momentum instead of losing it entirely.
 */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SeraphFlightController
{
    private static final double GRAVITY_PER_TICK = 0.06;
    private static final double DRAG = 0.98;
    private static final double DESCENDING_THRESHOLD = -0.15;
    private static final long FLAPPING_STATE_WINDOW_TICKS = 6;
    private static final int TAKEOFF_STATE_TICKS = 8;

    private static boolean inFlight = false;
    private static boolean wasJumpDown = false;
    private static long takeoffStartedTick = 0;
    private static long lastFlapTick = 0;
    private static SeraphFlightState state = SeraphFlightState.GROUNDED;

    private SeraphFlightController() {}

    public static boolean isInFlight()
    {
        return inFlight;
    }

    public static SeraphFlightState getState()
    {
        return state;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null)
            return;

        boolean jumpDown = mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !wasJumpDown;
        wasJumpDown = jumpDown;

        if (ClientRaceCache.get(player.getId()) != Race.SERAPH)
        {
            if (inFlight)
                endFlight();
            return;
        }

        if (!inFlight)
        {
            if (jumpPressed)
                beginFlight(player);
            else
                state = SeraphFlightState.GROUNDED;
            return;
        }

        if (player.onGround())
        {
            endFlight();
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double x = velocity.x * DRAG;
        double y = velocity.y - GRAVITY_PER_TICK * SeraphFlightConfig.FLIGHT_GRAVITY_MULTIPLIER.get();
        double z = velocity.z * DRAG;

        boolean thrusting = player.zza > 0 && foodAllows(player);
        Vec3 look = player.getLookAngle();
        double blendRate = thrusting ? SeraphFlightConfig.LOOK_STEER_BLEND_RATE.get() : SeraphFlightConfig.LOOK_STEER_GLIDE_BLEND_RATE.get();

        float angleFromStraightUp = Mth.degreesDifferenceAbs(player.getXRot(), -90.0F);
        if (angleFromStraightUp <= SeraphFlightConfig.CLIMB_BOOST_ANGLE_DEGREES.get())
            blendRate *= SeraphFlightConfig.CLIMB_BOOST_MULTIPLIER.get();

        double targetSpeed = SeraphFlightConfig.LOOK_STEER_TARGET_SPEED.get();
        x += (look.x * targetSpeed - x) * blendRate;
        y += (look.y * targetSpeed - y) * blendRate;
        z += (look.z * targetSpeed - z) * blendRate;

        if (jumpPressed && flapCooldownReady() && foodAllows(player))
        {
            double forward = SeraphFlightConfig.FLAP_FORWARD_FORCE.get();
            double upward = SeraphFlightConfig.FLAP_UPWARD_FORCE.get();
            x += look.x * forward;
            y += upward;
            z += look.z * forward;

            lastFlapTick = mc.level.getGameTime();
            SeraphFlapTracker.recordFlap(player.getId());
            BloodlinesNetwork.CHANNEL.sendToServer(new SeraphFlapPacket());
        }

        double maxSpeed = SeraphFlightConfig.MAX_FLIGHT_SPEED.get();
        double horizontalSpeed = Math.sqrt(x * x + z * z);
        if (horizontalSpeed > maxSpeed)
        {
            double scale = maxSpeed / horizontalSpeed;
            x *= scale;
            z *= scale;
        }

        double maxAscent = SeraphFlightConfig.MAX_ASCENT_SPEED.get();
        y = Math.max(-maxAscent * 2.0, Math.min(maxAscent, y));

        player.setDeltaMovement(x, y, z);
        player.resetFallDistance();

        updateState(mc.level.getGameTime(), y, thrusting);
    }

    private static void updateState(long gameTime, double verticalVelocity, boolean thrusting)
    {
        if (state == SeraphFlightState.TAKEOFF && gameTime - takeoffStartedTick <= TAKEOFF_STATE_TICKS)
            return;

        long ticksSinceFlap = gameTime - lastFlapTick;
        if (ticksSinceFlap < FLAPPING_STATE_WINDOW_TICKS)
            state = SeraphFlightState.FLAPPING;
        else if (verticalVelocity < DESCENDING_THRESHOLD)
            state = SeraphFlightState.DESCENDING;
        else if (thrusting)
            state = SeraphFlightState.ACTIVE_FLIGHT;
        else
            state = SeraphFlightState.GLIDING;
    }

    private static void beginFlight(LocalPlayer player)
    {
        inFlight = true;
        state = SeraphFlightState.TAKEOFF;
        takeoffStartedTick = player.level().getGameTime();

        Vec3 look = player.getLookAngle();
        Vec3 movement = player.getDeltaMovement();
        double forward = SeraphFlightConfig.TAKEOFF_FORWARD_FORCE.get();
        double upward = SeraphFlightConfig.TAKEOFF_UPWARD_FORCE.get();
        player.setDeltaMovement(movement.x + look.x * forward, upward, movement.z + look.z * forward);
        player.resetFallDistance();

        lastFlapTick = player.level().getGameTime();
        SeraphFlapTracker.recordFlap(player.getId());
        BloodlinesNetwork.CHANNEL.sendToServer(new SeraphFlapPacket());
    }

    private static void endFlight()
    {
        inFlight = false;
        state = SeraphFlightState.LANDING;
    }

    private static boolean flapCooldownReady()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return false;

        return mc.level.getGameTime() - lastFlapTick >= SeraphFlightConfig.FLAP_COOLDOWN_TICKS.get();
    }

    private static boolean foodAllows(LocalPlayer player)
    {
        if (SeraphFlightConfig.INDEFINITE_FLIGHT.get() || player.isCreative())
            return true;

        return player.getFoodData().getFoodLevel() >= SeraphFlightConfig.REQUIRED_FOOD_LEVEL.get();
    }
}
