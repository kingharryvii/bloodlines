package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the Fae's articulated wing rig (see FaeWingModel) frame by frame. No Icarus item involved: Fae's flight
 * is fully native (see RaceFlightResource), so this is gated purely on race.
 * <p>
 * The animation is built from two layers: a slowly-smoothed "open amount" (0=folded flat against the spine,
 * 1=fully spread) that covers idle/walking/jump-fall/landing entirely through interpolation - there's no
 * separate landing animation because easing the open amount back down to its idle target already produces one -
 * and, only while actively flying, a butterfly flap layered on top that's driven by an asymmetric wave (a quick
 * downstroke, a slower recovery) with the lower wing trailing the upper wing by a short phase delay.
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");

    private static final float BODY_SCALE = 1.3F;

    // How quickly the smoothed open amount chases its target each frame (higher = snappier).
    private static final float OPEN_SMOOTHING = 0.12F;

    // Target open amount (0=folded, 1=spread) per state.
    private static final float OPEN_IDLE = 0.05F;
    private static final float OPEN_WALKING = 0.18F;
    private static final float OPEN_AIRBORNE = 1.0F;

    // Folded pose: swept down vertically against the spine (rotation around Z takes the rest "outward" +X
    // direction toward +Y, which is down, in model space). Upper is angled a touch further than lower so it
    // visually wraps in front when they stack.
    private static final float FOLD_UPPER_BASE = 1.35F;
    private static final float FOLD_UPPER_MID = -0.15F;
    private static final float FOLD_UPPER_TIP = -0.2F;
    private static final float FOLD_LOWER_BASE = 1.5F;
    private static final float FOLD_LOWER_TIP = 0.15F;

    // Open/glide pose: swept out to the side. Upper curls up toward the tip, lower droops down toward its tip.
    private static final float OPEN_UPPER_BASE = -0.12F;
    private static final float OPEN_UPPER_MID = -0.15F;
    private static final float OPEN_UPPER_TIP = -0.25F;
    private static final float OPEN_LOWER_BASE = 0.35F;
    private static final float OPEN_LOWER_TIP = 0.15F;

    // Flap: added on top of the open pose while flying. Amplitude grows toward each chain's tip for a whip-like
    // follow-through; the lower chain's whole wave is phase-delayed behind the upper chain's.
    private static final float FLAP_SPEED = 1.6F;
    private static final float FLAP_LOWER_DELAY = 1.1F;
    private static final float FLAP_AMP_UPPER_BASE = 0.22F;
    private static final float FLAP_AMP_UPPER_MID = 0.32F;
    private static final float FLAP_AMP_UPPER_TIP = 0.42F;
    private static final float FLAP_AMP_LOWER_BASE = 0.18F;
    private static final float FLAP_AMP_LOWER_TIP = 0.28F;
    private static final float FLAP_PITCH_AMPLITUDE = 0.1F;

    // Idle sway / walking wiggle, layered on top of everything else.
    private static final float IDLE_SWAY_SPEED = 0.05F;
    private static final float IDLE_SWAY_AMPLITUDE = 0.02F;
    private static final float WALK_WIGGLE_AMPLITUDE = 0.06F;

    private static final Map<Integer, Float> OPEN_AMOUNT = new ConcurrentHashMap<>();

    private final FaeWingModel model;

    public FaeWingsLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet)
    {
        super(renderer);
        this.model = new FaeWingModel(modelSet.bakeLayer(FaeWingModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (ClientRaceCache.get(entity.getId()) != Race.FAE || entity.isInvisible())
            return;

        boolean flying = entity instanceof Player player && player.getAbilities().flying;
        Vec3 movement = entity.getDeltaMovement();
        boolean airborne = flying || !entity.onGround();
        boolean walking = !airborne && (Math.abs(movement.x) + Math.abs(movement.z) > 0.02);

        float target = airborne ? OPEN_AIRBORNE : (walking ? OPEN_WALKING : OPEN_IDLE);
        float openAmount = OPEN_AMOUNT.getOrDefault(entity.getId(), OPEN_IDLE);
        openAmount += (target - openAmount) * OPEN_SMOOTHING;
        OPEN_AMOUNT.put(entity.getId(), openAmount);

        float upperBase = lerp(openAmount, FOLD_UPPER_BASE, OPEN_UPPER_BASE);
        float upperMid = lerp(openAmount, FOLD_UPPER_MID, OPEN_UPPER_MID);
        float upperTip = lerp(openAmount, FOLD_UPPER_TIP, OPEN_UPPER_TIP);
        float lowerBase = lerp(openAmount, FOLD_LOWER_BASE, OPEN_LOWER_BASE);
        float lowerTip = lerp(openAmount, FOLD_LOWER_TIP, OPEN_LOWER_TIP);
        float pitch = 0.0F;

        if (!airborne)
        {
            float sway = walking
                    ? (float) Math.sin(limbSwing) * limbSwingAmount * WALK_WIGGLE_AMPLITUDE
                    : (float) Math.sin(ageInTicks * IDLE_SWAY_SPEED) * IDLE_SWAY_AMPLITUDE;
            upperBase += sway;
            lowerBase += sway * 0.7F;
        }

        if (flying)
        {
            float upperPhase = ageInTicks * FLAP_SPEED;
            float lowerPhase = upperPhase - FLAP_LOWER_DELAY;
            float upperWave = downstrokeWave(upperPhase);
            float lowerWave = downstrokeWave(lowerPhase);

            upperBase += upperWave * FLAP_AMP_UPPER_BASE;
            upperMid += upperWave * FLAP_AMP_UPPER_MID;
            upperTip += upperWave * FLAP_AMP_UPPER_TIP;
            lowerBase += lowerWave * FLAP_AMP_LOWER_BASE;
            lowerTip += lowerWave * FLAP_AMP_LOWER_TIP;
            pitch = (upperWave - 0.5F) * FLAP_PITCH_AMPLITUDE;
        }

        model.setPose(upperBase, upperMid, upperTip, lowerBase, lowerTip, pitch);

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    /**
     * A repeating 0..1 wave shaped like a wingbeat: a quick rise (the downstroke, {@link #DOWNSTROKE_FRACTION} of
     * the cycle) to 1, then a slower ease back down to 0 (the recovery stroke) - asymmetric on purpose, since a
     * linear/symmetric sine reads as robotic rather than like a living wing.
     */
    private static float downstrokeWave(float phase)
    {
        float t = phase / (2.0F * (float) Math.PI);
        t -= (float) Math.floor(t);

        if (t < DOWNSTROKE_FRACTION)
            return easeInOutQuad(t / DOWNSTROKE_FRACTION);
        else
            return 1.0F - easeInOutQuad((t - DOWNSTROKE_FRACTION) / (1.0F - DOWNSTROKE_FRACTION));
    }

    private static final float DOWNSTROKE_FRACTION = 0.35F;

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
