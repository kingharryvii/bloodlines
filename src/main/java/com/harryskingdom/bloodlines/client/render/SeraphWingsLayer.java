package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.race.SeraphFlapTracker;
import com.harryskingdom.bloodlines.client.race.SeraphFlightController;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Draws Seraph's wings - reuses vanilla's own ElytraLayer/ElytraModel with a custom texture, the same technique
 * that ended up working for Fae after a custom model failed to look right, instead of the bespoke 4-wing cuboid
 * rig this class started as (also abandoned for the same reason: it didn't read as real wings in motion). No
 * Icarus item involved: flight is fully native (see SeraphFlightController), so this is gated purely on race.
 * <p>
 * Two animation layers, both driving elytraRotX/Y/Z: a continuous automatic wingbeat that plays for the entire
 * duration of flight (real reference footage - Icarus + Medieval Origins Revival's Valkyrie - confirmed the
 * wings keep beating throughout flight, not just on a manual input; per-flight-tick control is a Bloodlines
 * addition for lift, not something the wings should wait around for), plus a one-shot fast-downstroke/slower-
 * recovery boost layered on top read from SeraphFlapTracker's actual flap events, so a real flap still reads as
 * a stronger, distinct beat rather than blending invisibly into the ambient cycle. Works identically for the
 * local player and every other visible Seraph, since SeraphFlapTracker is kept in sync for both (see
 * SyncSeraphFlapPacket).
 */
public class SeraphWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/seraph_wings.png");
    private static final float WING_SCALE = 1.5F;

    private static final float OPEN_ELYTRA_ROT_X = 0.8981317F;
    private static final float OPEN_ELYTRA_ROT_Y = 0.58726646F;
    private static final float OPEN_ELYTRA_ROT_Z = -0.5F - (float) Math.PI / 4F;
    private static final float OPEN_EASING = 0.25F;

    // Continuous ambient wingbeat, active for the whole flight - slower/more majestic than Fae's fast fairy
    // flutter, matching a large powerful creature rather than an insect.
    private static final float AUTO_FLAP_SPEED = 2.8F;
    private static final float AUTO_FLAP_AMPLITUDE_X = 0.35F;
    private static final float AUTO_FLAP_AMPLITUDE_Z = 0.2F;

    // One-shot boost layered on top for an actual flap event, so a real flap still reads as a distinct, stronger
    // beat rather than disappearing into the ambient cycle.
    private static final float BOOST_AMPLITUDE_X = 0.3F;
    private static final float BOOST_AMPLITUDE_Z = 0.15F;
    private static final float DOWNSTROKE_TICKS = 3.5F;
    private static final float RECOVERY_TICKS = 7.0F;

    /** How long after landing (no recent flap) a remote player's wings still read as "open". */
    private static final long GROUNDED_LINGER_TICKS = 10;

    public SeraphWingsLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet)
    {
        super(renderer, modelSet);
    }

    @Override
    public boolean shouldRender(ItemStack stack, T entity)
    {
        return ClientRaceCache.get(entity.getId()) == Race.SERAPH;
    }

    @Override
    public ResourceLocation getElytraTexture(ItemStack stack, T entity)
    {
        return TEXTURE;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(WING_SCALE, WING_SCALE, WING_SCALE);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        poseStack.popPose();

        // Runs after super.render() so this frame's already-rendered pose isn't touched - only the next frame's
        // starting point is nudged, matching vanilla's own ElytraModel.setupAnim easing timing.
        if (entity instanceof AbstractClientPlayer player && ClientRaceCache.get(entity.getId()) == Race.SERAPH)
        {
            boolean isLocalPlayer = Minecraft.getInstance().player == player;
            boolean flying = isLocalPlayer
                    ? SeraphFlightController.isInFlight()
                    : (!entity.onGround() || SeraphFlapTracker.ticksSinceFlap(entity.getId()) < GROUNDED_LINGER_TICKS);

            if (flying)
            {
                float autoFlap = (float) Math.sin(ageInTicks * AUTO_FLAP_SPEED);

                float t = (float) SeraphFlapTracker.ticksSinceFlap(entity.getId()) + partialTicks;
                float boost = flapEnvelope(t);

                float targetX = OPEN_ELYTRA_ROT_X + autoFlap * AUTO_FLAP_AMPLITUDE_X + boost * BOOST_AMPLITUDE_X;
                float targetY = OPEN_ELYTRA_ROT_Y;
                float targetZ = OPEN_ELYTRA_ROT_Z + autoFlap * AUTO_FLAP_AMPLITUDE_Z + boost * BOOST_AMPLITUDE_Z;

                player.elytraRotX += (targetX - player.elytraRotX) * OPEN_EASING;
                player.elytraRotY += (targetY - player.elytraRotY) * OPEN_EASING;
                player.elytraRotZ += (targetZ - player.elytraRotZ) * OPEN_EASING;
            }
        }
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
}
