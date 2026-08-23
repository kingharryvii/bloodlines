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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float WING_SCALE = 1.4F;

    private static final float OPEN_ELYTRA_ROT_X = 0.8981317F;
    private static final float OPEN_ELYTRA_ROT_Y = 0.58726646F;
    private static final float OPEN_ELYTRA_ROT_Z = -0.5F - (float) Math.PI / 4F;
    private static final float OPEN_EASING = 0.25F;

    private static final float FLAP_SPEED = 5.8F;
    private static final float FLAP_AMPLITUDE_X = 0.35F;
    private static final float FLAP_AMPLITUDE_Z = 0.2F;

    private static final double GROUND_DISTANCE_THRESHOLD = 0.1;

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
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(WING_SCALE, WING_SCALE, WING_SCALE);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);
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

    /** 0 if not jumping/falling/gliding fast enough to warrant a flap, otherwise how strongly to flap (0..1). */
    private static float fallFlapStrength(AbstractClientPlayer player)
    {
        Vec3 movement = player.getDeltaMovement();
        if (movement.y < -0.5)
            return 0;

        double normalizedY = movement.y * 2.5;
        double speedMagnitude = Math.sqrt(movement.x * movement.x + movement.z * movement.z + Math.max(normalizedY, 0) * Math.max(normalizedY, 0)) * 4;
        float flapStrength = (float) Math.min(speedMagnitude, 1.0);

        Vec3 start = player.position();
        Vec3 end = start.add(0, -1, 0);
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean closeToGround = hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(start) <= GROUND_DISTANCE_THRESHOLD;

        boolean shouldFlap = player.isFallFlying() || normalizedY > 0.1
                || (!closeToGround && (Math.abs(movement.x) + Math.abs(movement.z) > 0.1));
        return shouldFlap ? flapStrength : 0;
    }
}
