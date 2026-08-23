package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.race.SeraphFlapTracker;
import com.harryskingdom.bloodlines.client.race.SeraphFlightController;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.seraph.SeraphFlightState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The "wing animator" half of Seraph's flight system - reads state SeraphFlightController/SeraphFlapTracker
 * produce and poses the wings accordingly, but never touches physics itself. Works identically for the local
 * player and every other visible Seraph: the local player's own controller updates SeraphFlapTracker with zero
 * latency, and SyncSeraphFlapPacket keeps remote players' entries updated too, so this layer only ever needs to
 * read one shared source of truth regardless of whose wings it's drawing.
 * <p>
 * Two independent animation layers, matching the "wing behavior" spec exactly:
 * <ol>
 * <li>A smoothed open/fold amount (0=folded flat against the back, 1=fully spread) that alone covers the
 * take-off/landing transitions - there's no separate takeoff/landing animation because easing this value toward
 * its target already produces one.</li>
 * <li>A per-flap envelope: a one-shot asymmetric curve (fast downstroke, slower recovery) that plays out once
 * per real flap event read from SeraphFlapTracker, not a free-running oscillator - the wings only move when a
 * flap actually happened, with the lower wings evaluating the same curve on a short delay behind the uppers.</li>
 * </ol>
 */
public class SeraphWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/seraph_wings.png");

    private static final float OPEN_SMOOTHING = 0.12F;

    // Rest pose while folded (grounded/idle) - Z sweeps from "outward" toward "down the spine".
    private static final float FOLD_UPPER_BASE = 1.3F;
    private static final float FOLD_UPPER_TIP = -0.15F;
    private static final float FOLD_LOWER_BASE = 1.45F;
    private static final float FOLD_LOWER_TIP = 0.1F;

    // Rest pose while airborne, before any flap offset is added.
    private static final float OPEN_UPPER_BASE = -0.05F;
    private static final float OPEN_UPPER_TIP = -0.25F;
    private static final float OPEN_LOWER_BASE = 0.25F;
    private static final float OPEN_LOWER_TIP = 0.15F;

    // Added on top of the open pose, scaled by the flap envelope (0..1).
    private static final float FLAP_UPPER_BASE = 0.55F;
    private static final float FLAP_UPPER_TIP = 0.45F;
    private static final float FLAP_LOWER_BASE = 0.4F;
    private static final float FLAP_LOWER_TIP = 0.35F;
    private static final float FLAP_PITCH = 0.15F;

    private static final float DOWNSTROKE_TICKS = 3.5F;
    private static final float RECOVERY_TICKS = 7.0F;
    private static final float LOWER_WING_DELAY_TICKS = 2.0F;

    /** How long after landing (no controller, no recent flap) a remote player's wings still read as "open". */
    private static final long GROUNDED_LINGER_TICKS = 10;

    private static final Map<Integer, Float> OPEN_AMOUNT = new ConcurrentHashMap<>();

    private final SeraphWingModel model;

    public SeraphWingsLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet)
    {
        super(renderer);
        this.model = new SeraphWingModel(modelSet.bakeLayer(SeraphWingModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (ClientRaceCache.get(entity.getId()) != Race.SERAPH || entity.isInvisible())
            return;

        boolean isLocalPlayer = Minecraft.getInstance().player == entity;
        boolean flying = isLocalPlayer
                ? SeraphFlightController.isInFlight()
                : (!entity.onGround() || SeraphFlapTracker.ticksSinceFlap(entity.getId()) < GROUNDED_LINGER_TICKS);

        float target = flying ? 1.0F : 0.0F;
        float openAmount = OPEN_AMOUNT.getOrDefault(entity.getId(), 0.0F);
        openAmount += (target - openAmount) * OPEN_SMOOTHING;
        OPEN_AMOUNT.put(entity.getId(), openAmount);

        float upperBase = lerp(openAmount, FOLD_UPPER_BASE, OPEN_UPPER_BASE);
        float upperTip = lerp(openAmount, FOLD_UPPER_TIP, OPEN_UPPER_TIP);
        float lowerBase = lerp(openAmount, FOLD_LOWER_BASE, OPEN_LOWER_BASE);
        float lowerTip = lerp(openAmount, FOLD_LOWER_TIP, OPEN_LOWER_TIP);
        float pitch = 0.0F;

        if (flying)
        {
            float upperT = (float) SeraphFlapTracker.ticksSinceFlap(entity.getId()) + partialTicks;
            float lowerT = upperT - LOWER_WING_DELAY_TICKS;
            float upperEnvelope = flapEnvelope(upperT);
            float lowerEnvelope = flapEnvelope(lowerT);

            upperBase += upperEnvelope * FLAP_UPPER_BASE;
            upperTip += upperEnvelope * FLAP_UPPER_TIP;
            lowerBase += lowerEnvelope * FLAP_LOWER_BASE;
            lowerTip += lowerEnvelope * FLAP_LOWER_TIP;
            pitch = upperEnvelope * FLAP_PITCH;
        }

        model.setPose(upperBase, upperTip, lowerBase, lowerTip, pitch);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    /**
     * One-shot 0..1 envelope for a single flap: rises fast (the downstroke) then eases back down slower (the
     * recovery), settling at 0 - not a repeating wave, so the wings only move in response to an actual flap.
     */
    private static float flapEnvelope(float ticksSincePeak)
    {
        if (ticksSincePeak < 0)
            return 0.0F;

        if (ticksSincePeak < DOWNSTROKE_TICKS)
            return easeOutQuad(ticksSincePeak / DOWNSTROKE_TICKS);

        float recoveryT = (ticksSincePeak - DOWNSTROKE_TICKS) / RECOVERY_TICKS;
        if (recoveryT >= 1.0F)
            return 0.0F;

        return 1.0F - easeInOutQuad(recoveryT);
    }

    private static float easeOutQuad(float t)
    {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static float easeInOutQuad(float t)
    {
        return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0 * t + 2.0, 2.0) / 2.0F;
    }

    private static float lerp(float t, float from, float to)
    {
        return from + (to - from) * t;
    }

    /** Drops the smoothed animation state for a player that's gone (logout, race change cleanup, etc). */
    public static void clearState(int entityId)
    {
        OPEN_AMOUNT.remove(entityId);
    }
}
