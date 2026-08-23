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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Seraph's flight controller: the "flight" half of the two connected systems the wing animation is built on top
 * of (see SeraphWingsLayer for the "wing animator" half, which only ever reads state this class produces - it
 * never drives physics itself). Runs client-side, local player only, since movement in this codebase is already
 * client-authoritative for both vanilla flight and Fae's native flight (the server trusts the resulting position
 * updates the same way it already does for creative/elytra flight).
 * <p>
 * Deliberately does NOT hook real vanilla fall-flying (isFallFlying/DATA_SHARED_FLAGS_ID): there's no public API
 * to trigger it externally in 1.20.1 (the closest, LivingEntity.updateFallFlying, is private, and the shared-flag
 * accessor is protected), and reaching for a Mixin or Access Transformer just to borrow vanilla's hardcoded
 * gravity/drag numbers isn't worth it when every one of those numbers needs to be configurable anyway. Instead
 * this hand-rolls elytra-style physics (gravity, drag, look-direction steering) driven entirely by
 * SeraphFlightConfig, then layers jump-triggered flap impulses on top - "elytra movement + Icarus-style active
 * flapping" implemented directly rather than borrowed from vanilla's fixed formula.
 */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SeraphFlightController
{
    private static final double GRAVITY_PER_TICK = 0.08;
    private static final double DRAG = 0.98;
    private static final double STEERING_STRENGTH = 0.08;
    private static final long ACTIVE_FLIGHT_WINDOW_TICKS = 20;
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
                endFlight(player);
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
            endFlight(player);
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        double x = movement.x * DRAG;
        double y = movement.y - GRAVITY_PER_TICK * SeraphFlightConfig.FLIGHT_GRAVITY_MULTIPLIER.get();
        double z = movement.z * DRAG;

        Vec3 look = player.getLookAngle();
        x += look.x * STEERING_STRENGTH;
        y += look.y * STEERING_STRENGTH;
        z += look.z * STEERING_STRENGTH;

        boolean flapped = false;
        if (jumpPressed && flapCooldownReady() && foodAllows(player))
        {
            double forward = SeraphFlightConfig.FLAP_FORWARD_FORCE.get();
            double upward = SeraphFlightConfig.FLAP_UPWARD_FORCE.get();
            x += look.x * forward;
            y += upward;
            z += look.z * forward;

            lastFlapTick = mc.level.getGameTime();
            flapped = true;
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

        long ticksSinceLastFlap = mc.level.getGameTime() - lastFlapTick;
        if (state == SeraphFlightState.TAKEOFF)
        {
            if (mc.level.getGameTime() - takeoffStartedTick > TAKEOFF_STATE_TICKS)
                state = SeraphFlightState.ACTIVE_FLIGHT;
        }
        else
        {
            state = ticksSinceLastFlap < ACTIVE_FLIGHT_WINDOW_TICKS ? SeraphFlightState.ACTIVE_FLIGHT : SeraphFlightState.GLIDE;
        }
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

    private static void endFlight(LocalPlayer player)
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
