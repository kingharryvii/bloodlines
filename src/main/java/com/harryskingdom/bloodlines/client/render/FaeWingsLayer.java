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
 * "flying upright". BloodlinesClientEvents.updateFaeWingFlap used to fight that every tick by hard-*setting*
 * elytraRotX to 1.4981317F while Abilities.flying - a rigid, discontinuous jump vanilla's own per-frame pull
 * would immediately start dragging back down, producing a snap to an extreme angle each tick rather than a
 * settled open pose (that's what "wings coming out of the head" was).
 * <p>
 * Simply easing toward one fixed open target instead (an earlier version of this fix) solved that but also
 * removed all motion while flying - the wings just glide open once and sit still, since a constant target has no
 * oscillation to visibly track. So the target itself now oscillates (a sine wave on X/Z driven by ageInTicks),
 * kept comfortably clear of the angle that caused the head-poking, giving a real, continuous wingbeat instead of
 * either a static pose or a periodic snap.
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float WING_SCALE = 1.1F;

    private static final float OPEN_ELYTRA_ROT_X = 0.8981317F;
    private static final float OPEN_ELYTRA_ROT_Y = 0.58726646F;
    private static final float OPEN_ELYTRA_ROT_Z = -0.5F - (float) Math.PI / 4F;
    private static final float OPEN_EASING = 0.1F;

    private static final float FLAP_SPEED = 1.4F;
    private static final float FLAP_AMPLITUDE_X = 0.35F;
    private static final float FLAP_AMPLITUDE_Z = 0.2F;

    // Vanilla's own elytra pivot sits at the neck/shoulder line (y=0 in its model space); nudged down to sit
    // further down the back instead.
    private static final float VERTICAL_OFFSET = 0.2F;

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
        poseStack.translate(0, VERTICAL_OFFSET, 0);
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        poseStack.popPose();

        // Runs after super.render() so this frame's pose (just set by vanilla's own ElytraModel.setupAnim) isn't
        // touched - only the NEXT frame's starting point is nudged toward the open target, same timing as
        // Medieval's own mixin injecting at ElytraModel.setupAnim's RETURN.
        if (entity instanceof AbstractClientPlayer player && player.getAbilities().flying
                && ClientRaceCache.get(entity.getId()) == Race.FAE)
        {
            float flap = (float) Math.sin(ageInTicks * FLAP_SPEED);
            player.elytraRotX += (OPEN_ELYTRA_ROT_X + flap * FLAP_AMPLITUDE_X - player.elytraRotX) * OPEN_EASING;
            player.elytraRotY += (OPEN_ELYTRA_ROT_Y - player.elytraRotY) * OPEN_EASING;
            player.elytraRotZ += (OPEN_ELYTRA_ROT_Z + flap * FLAP_AMPLITUDE_Z - player.elytraRotZ) * OPEN_EASING;
        }
    }
}
