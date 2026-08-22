package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
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
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends ElytraLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float WING_SCALE = 1.1F;

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
    }
}
