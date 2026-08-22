package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the Fae's wings on Fae players. Currently a silhouette-approval checkpoint: FaeWingModel renders a fixed,
 * fully-open stationary pose with no folding/flapping. Once the shape is approved, this is where the open-amount
 * smoothing and flap state machine come back (see git history for the previous version of this class).
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");
    private static final float BODY_SCALE = 1.3F;

    private final FaeWingModel model = new FaeWingModel();

    public FaeWingsLayer(RenderLayerParent<T, M> renderer)
    {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (ClientRaceCache.get(entity.getId()) != Race.FAE || entity.isInvisible())
            return;

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.render(poseStack, vertexConsumer, packedLight);

        poseStack.popPose();
    }
}
