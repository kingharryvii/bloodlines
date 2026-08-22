package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.integration.pehkui.PehkuiIntegration;
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
 * settled open pose (that's what "wings coming out of the head" was). Fixed by dropping that hard set entirely
 * and instead easing here, every frame, toward the same moderate open target Medieval Origins Revival's own
 * ElytraModelMixin uses - smooth and continuous instead of a periodic snap, and gated on actually flying so
 * idle/walking still settle toward vanilla's own small default.
 * <p>
 * fae_wings.png is Medieval Origins Revival's own pixie_wings.png (CC BY 4.0, credit muon-rw - see
 * mods.toml's credits field for the required attribution), an interim placeholder pending commissioned art.
 * <p>
 * Pehkui shrinks Fae's actual body model directly (its bones), not via an outer PoseStack wrap around the whole
 * entity render - so this layer, a separate model entirely, doesn't shrink along with it automatically. Without
 * accounting for that, the wings render at full (unscaled) size against a visibly tiny body, reading as
 * oversized and detached. See PehkuiIntegration.getVisualScale.
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float WING_SCALE = 1.1F;

    private static final float OPEN_ELYTRA_ROT_X = 0.8981317F;
    private static final float OPEN_ELYTRA_ROT_Y = 0.58726646F;
    private static final float OPEN_ELYTRA_ROT_Z = -0.5F - (float) Math.PI / 4F;
    private static final float OPEN_EASING = 0.1F;

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
        float effectiveScale = WING_SCALE * PehkuiIntegration.getVisualScale(entity, partialTicks);

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(effectiveScale, effectiveScale, effectiveScale);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        poseStack.popPose();

        // Runs after super.render() so this frame's pose (just set by vanilla's own ElytraModel.setupAnim) isn't
        // touched - only the NEXT frame's starting point is nudged toward the open target, same timing as
        // Medieval's own mixin injecting at ElytraModel.setupAnim's RETURN.
        if (entity instanceof AbstractClientPlayer player && player.getAbilities().flying
                && ClientRaceCache.get(entity.getId()) == Race.FAE)
        {
            player.elytraRotX += (OPEN_ELYTRA_ROT_X - player.elytraRotX) * OPEN_EASING;
            player.elytraRotY += (OPEN_ELYTRA_ROT_Y - player.elytraRotY) * OPEN_EASING;
            player.elytraRotZ += (OPEN_ELYTRA_ROT_Z - player.elytraRotZ) * OPEN_EASING;
        }
    }
}
