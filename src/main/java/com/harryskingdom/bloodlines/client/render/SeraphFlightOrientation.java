package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.race.SeraphFlapTracker;
import com.harryskingdom.bloodlines.client.race.SeraphFlightController;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tilts a flying Seraph's whole body forward into the flight direction, so they read as an actual flying
 * creature instead of a person standing upright in midair (confirmed from the user's own test footage: body
 * stayed fully vertical in every frame, wings just drooping behind a standing pose).
 * <p>
 * Real vanilla fall-flying gets this for free - LivingEntityRenderer's own setupRotations tilts the model
 * whenever isFallFlying() is true, which is exactly what Medieval Origins Revival's Valkyrie relies on (their
 * "wings" power is literally medievalorigins:icarus_wings, which just grants real Icarus's white_feathered_wings
 * item with retuned config - it's not a custom flight system, it's real Icarus). Since Bloodlines' Seraph
 * deliberately isn't hooking real fall-flying (see SeraphFlightController's javadoc for why), that free tilt
 * doesn't happen automatically and has to be added back explicitly.
 * <p>
 * RenderLayers (like SeraphWingsLayer) can't do this themselves - they only run after the base body model is
 * already posed. RenderLivingEvent.Pre fires before that, with access to the same PoseStack the body and all
 * layers render into, so a rotation pushed here affects the whole entity, not just a layer's own drawing.
 */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SeraphFlightOrientation
{
    // Forward lean while cruising level (not looking up or down) - enough to clearly read as "flying creature",
    // short of vanilla elytra's much more extreme prone dive.
    private static final float BASE_CRUISE_TILT_DEGREES = 35.0F;
    private static final float PITCH_INFLUENCE = 0.5F;
    private static final float MIN_TILT_DEGREES = 10.0F;
    private static final float MAX_TILT_DEGREES = 75.0F;
    private static final float TILT_SMOOTHING = 0.12F;

    /** How long after landing (no recent flap) a remote player still counts as flying for tilt purposes. */
    private static final long GROUNDED_LINGER_TICKS = 10;

    private static final Map<Integer, Float> CURRENT_TILT = new ConcurrentHashMap<>();

    private SeraphFlightOrientation() {}

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event)
    {
        LivingEntity entity = event.getEntity();
        if (ClientRaceCache.get(entity.getId()) != Race.SERAPH)
            return;

        boolean isLocalPlayer = Minecraft.getInstance().player == entity;
        boolean flying = isLocalPlayer
                ? SeraphFlightController.isInFlight()
                : (!entity.onGround() || SeraphFlapTracker.ticksSinceFlap(entity.getId()) < GROUNDED_LINGER_TICKS);

        float target = 0.0F;
        if (flying)
        {
            target = BASE_CRUISE_TILT_DEGREES + entity.getViewXRot(event.getPartialTick()) * PITCH_INFLUENCE;
            target = Math.max(MIN_TILT_DEGREES, Math.min(MAX_TILT_DEGREES, target));
        }

        float current = CURRENT_TILT.getOrDefault(entity.getId(), 0.0F);
        current += (target - current) * TILT_SMOOTHING;
        CURRENT_TILT.put(entity.getId(), current);

        if (Math.abs(current) < 0.01F)
            return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(current));
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);
    }
}
