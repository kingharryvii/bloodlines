package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws a race-specific custom wing model (currently just Fae - see BloodlinesClientSetup). Race is looked up
 * from ClientRaceCache (synced by SyncPlayerRacePacket) rather than the equipped Icarus wing item, so this stays
 * correct regardless of which Icarus wing variant IcarusIntegration happens to equip. Icarus's own render of
 * that equipped item is suppressed via IcarusAPIClient.addRenderPredicate (see BloodlinesClientSetup), so this
 * is the only wing actually drawn for Fae.
 */
public class RacialWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
    private final RaceWingModel<T> wingModel;
    private final Race race;
    private final ResourceLocation texture;

    public RacialWingsLayer(RenderLayerParent<T, M> renderer, ModelPart wingRoot, Race race, ResourceLocation texture)
    {
        super(renderer);
        this.wingModel = new RaceWingModel<>(wingRoot);
        this.race = race;
        this.texture = texture;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (ClientRaceCache.get(entity.getId()) != race)
            return;

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.125);
        wingModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = buffer.getBuffer(wingModel.renderType(texture));
        wingModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        poseStack.popPose();
    }
}
