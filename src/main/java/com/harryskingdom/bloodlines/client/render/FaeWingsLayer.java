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
 * Medieval Origins Revival (whose flap logic BloodlinesClientEvents.updateFaeWingFlap already ports) actually
 * combines two systems, not one: that tick-based logic which hard-sets elytraRotX/Y/Z to state-driven targets
 * every 2 ticks, AND a second pass - their ElytraModelMixin, injected into ElytraModel.setupAnim every render
 * frame - that continuously, unconditionally eases those same fields toward a gentler fixed resting pose. Without
 * that second pass the tick-set values (elytraRotX up to 1.4981317F while flying) stay rigidly pinned, which is
 * what made the wings snap into that extreme "coming out of the head" look. We don't use Mixins in this mod, but
 * this layer's render() already runs every frame ElytraModel.setupAnim would, so the same easing is applied here
 * instead, right before delegating to vanilla's own ElytraLayer/ElytraModel rendering.
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float WING_SCALE = 1.1F;

    private static final float RESTING_ELYTRA_ROT_X = 0.8981317F;
    private static final float RESTING_ELYTRA_ROT_Y = 0.58726646F;
    private static final float RESTING_ELYTRA_ROT_Z = -0.5F - (float) Math.PI / 4F;
    private static final float RESTING_EASING = 0.1F;

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
        if (entity instanceof AbstractClientPlayer player && ClientRaceCache.get(entity.getId()) == Race.FAE)
        {
            player.elytraRotX += (RESTING_ELYTRA_ROT_X - player.elytraRotX) * RESTING_EASING;
            player.elytraRotY += (RESTING_ELYTRA_ROT_Y - player.elytraRotY) * RESTING_EASING;
            player.elytraRotZ += (RESTING_ELYTRA_ROT_Z - player.elytraRotZ) * RESTING_EASING;
        }

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(WING_SCALE, WING_SCALE, WING_SCALE);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);
        super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        poseStack.popPose();
    }
}
