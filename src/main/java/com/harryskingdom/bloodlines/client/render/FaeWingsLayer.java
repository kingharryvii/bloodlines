package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a butterfly-shaped elytra model on Fae players - reuses vanilla's own ElytraLayer/ElytraModel with a
 * custom texture, so the geometry/UV/positioning is vanilla's own already-correct elytra shape, not anything
 * built from scratch. No Icarus item involved at all: Fae's flight is now fully native (see RaceFlightFood),
 * so this is gated purely on race. Rendered at WING_SCALE so they read as a dramatic wingspan rather than
 * getting lost against the (separately Pehkui-shrunk) Fae body.
 * <p>
 * Vanilla's own ElytraModel.setupAnim (decompiled and verified directly, not assumed from memory) already eases
 * player.elytraRotX/Y/Z toward a target every render frame - just a small, mostly-closed one (0.2617994F, 0,
 * -0.2617994F) whenever the entity isn't actually fall-flying or crouching, since vanilla has no concept of
 * "flying upright". A single sine-wave-driven flap (X/Z, riding on the same open target Medieval Origins
 * Revival's own mixin eases toward) now covers every airborne state - flying, jumping, falling - instead of
 * flying using one system and jumping/falling using a separate flapStrength-scaled static-target one; the
 * jump/fall case just scales the same wave's amplitude by how fast the player is actually moving, so it stays
 * responsive to real movement while keeping the same wingbeat character everywhere.
 * <p>
 * fae_wings.png is Medieval Origins Revival's own pixie_wings.png (CC BY 4.0, credit muon-rw - see
 * mods.toml's credits field for the required attribution), an interim placeholder pending commissioned art.
 * <p>
 * WING_SCALE deliberately does NOT compensate for Pehkui's body shrink (tried that, wings ended up too tiny -
 * the user wants the "too-big-for-a-tiny-fairy" look, not a proportionally-shrunk one) - it's a fixed constant
 * tuned by eye instead.
 * <p>
 * WING_SCALE (1.33 -> 1.1) and OPEN_ELYTRA_ROT_Y (0.587 -> 0.35 -> 0.18, roughly 33.6 degrees down to 20 down
 * to ~10) all trimmed back per user feedback that the two wing halves read as visibly separate pieces rather
 * than one cohesive wing set - confirmed still gapped from directly overhead even after the first Y-spread
 * cut, so cut again - less outward Y-spread keeps them closer together at rest, and the smaller overall scale
 * keeps that closer pair from looking oversized once they're not spread as far apart.
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float WING_SCALE = 1.1F;
    /**
     * Scaling around the vertical center of the whole bounding box (0.5) dragged the wing base away from the
     * actual shoulder attachment as it enlarged - "wings touching the back, then another pair floating apart"
     * in testing, since scaling around a point well below the shoulder pushes anything above that point (the
     * wings) further from it. 0.8 sits close to where vanilla's own ElytraModel actually attaches, so scaling
     * around it barely moves the base at all - only the tips spread out, which is the effect actually wanted.
     */
    private static final float SCALE_PIVOT_FRACTION = 0.8F;

    private static final float OPEN_ELYTRA_ROT_X = 0.8981317F;
    private static final float OPEN_ELYTRA_ROT_Y = 0.18F;
    private static final float OPEN_ELYTRA_ROT_Z = -0.5F - (float) Math.PI / 4F;
    private static final float OPEN_EASING = 0.25F;

    private static final float FLAP_SPEED = 5.2F;
    private static final float FLAP_AMPLITUDE_X = 0.35F;
    private static final float FLAP_AMPLITUDE_Z = 0.2F;

    /** Baseline flap strength whenever airborne at all, so standing still in midair still visibly flaps. */
    private static final float IDLE_AIRBORNE_FLAP_STRENGTH = 0.5F;

    public FaeWingsLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet)
    {
        super(renderer, modelSet);
    }

    @Override
    public boolean shouldRender(ItemStack stack, T entity)
    {
        return ClientRaceCache.get(entity.getId()) == Race.FAE;
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
        poseStack.translate(0, entity.getBbHeight() * SCALE_PIVOT_FRACTION, 0);
        poseStack.scale(WING_SCALE, WING_SCALE, WING_SCALE);
        poseStack.translate(0, -entity.getBbHeight() * SCALE_PIVOT_FRACTION, 0);
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        poseStack.popPose();

        // Runs after super.render() so this frame's pose (just set by vanilla's own ElytraModel.setupAnim) isn't
        // touched - only the NEXT frame's starting point is nudged toward the open target, same timing as
        // Medieval's own mixin injecting at ElytraModel.setupAnim's RETURN.
        if (entity instanceof AbstractClientPlayer player && ClientRaceCache.get(entity.getId()) == Race.FAE)
        {
            float flapStrength = player.getAbilities().flying ? 1.0F : fallFlapStrength(player);
            if (flapStrength > 0)
            {
                float flap = (float) Math.sin(ageInTicks * FLAP_SPEED);
                float targetX = (OPEN_ELYTRA_ROT_X + flap * FLAP_AMPLITUDE_X) * flapStrength;
                float targetY = OPEN_ELYTRA_ROT_Y * flapStrength;
                float targetZ = (OPEN_ELYTRA_ROT_Z + flap * FLAP_AMPLITUDE_Z) * flapStrength;

                player.elytraRotX += (targetX - player.elytraRotX) * OPEN_EASING;
                player.elytraRotY += (targetY - player.elytraRotY) * OPEN_EASING;
                player.elytraRotZ += (targetZ - player.elytraRotZ) * OPEN_EASING;
            }
        }
    }

    /**
     * 0 if standing on the ground or plummeting too fast to look right, otherwise how strongly to flap (0..1).
     * Used to gate requiring active horizontal/vertical movement to flap at all, which meant a Fae standing
     * still in midair - e.g. just drifting down under their own always-on slow falling, not pressing any
     * movement key - never flapped. Airborne-and-not-falling-hard is now enough on its own; movement only
     * scales the flap up from a gentle idle baseline rather than gating whether it happens at all.
     */
    private static float fallFlapStrength(AbstractClientPlayer player)
    {
        if (player.onGround())
            return 0;

        Vec3 movement = player.getDeltaMovement();
        if (movement.y < -0.5)
            return 0;

        double normalizedY = movement.y * 2.5;
        double speedMagnitude = Math.sqrt(movement.x * movement.x + movement.z * movement.z + Math.max(normalizedY, 0) * Math.max(normalizedY, 0)) * 4;
        return (float) Math.min(Math.max(speedMagnitude, IDLE_AIRBORNE_FLAP_STRENGTH), 1.0);
    }
}
