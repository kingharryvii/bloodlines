package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.model.HeadAccessoryModel;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Renders a small head-mounted accessory (Angelkin's halo, Demonkin's horns, elf ears on all three elf
 * subraces, Beastkin's cat ears), gated on race and attached by applying the parent HumanoidModel's own head
 * ModelPart transform first - the accessory then follows head turning/nodding for free, the same trick vanilla
 * itself uses for anything head-mounted. Reading the head's pose here (rather than writing to it, like the
 * Merfolk leg-hide had to) is always safe from a RenderLayer even though layers run after the base model's own
 * setupAnim - the head's rotation for this frame was already set moments earlier in the same frame, so there's
 * no stale-by-a-frame issue here.
 */
public class HeadAccessoryLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M>
{
    private final HeadAccessoryModel<T> model;
    private final ResourceLocation texture;
    private final Set<Race> races;

    public HeadAccessoryLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet, ModelLayerLocation layerLocation,
            String textureName, boolean spin, Race... races)
    {
        super(renderer);
        this.model = new HeadAccessoryModel<>(modelSet.bakeLayer(layerLocation), spin);
        this.texture = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/" + textureName + ".png");
        this.races = EnumSet.copyOf(Arrays.asList(races));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (!races.contains(ClientRaceCache.get(entity.getId())))
            return;

        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);

        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        poseStack.popPose();
    }
}
