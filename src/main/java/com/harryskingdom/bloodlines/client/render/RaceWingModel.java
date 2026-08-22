package com.harryskingdom.bloodlines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;

/**
 * A single flat wing panel per side, mirrored for the opposite wing. The texture is the real wing art pasted
 * twice side-by-side (front-face copy + back-face copy) so the art shows correctly whichever side the wing is
 * viewed from, since a zero-depth box's "front" and "back" faces sample two separate UV regions.
 * <p>
 * Posed each frame from the same public elytraRotX/Y/Z fields vanilla's own ElytraModel reads for its flap
 * animation - BloodlinesClientEvents already drives those fields, this model just applies them directly instead
 * of piggybacking on vanilla's own elytra shape/UV.
 */
public class RaceWingModel<T extends LivingEntity> extends EntityModel<T>
{
    private static final float BASE_X = 0.1F;
    private static final float BASE_Y = 0.25F;
    private static final float BASE_Z = 0.35F;

    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public RaceWingModel(ModelPart root)
    {
        super(RenderType::entityTranslucent);
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer(float wingWidth, float wingHeight)
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // y=0 in this space is roughly neck/shoulder height, with +y pointing DOWN toward the feet - so the box
        // must span [0, wingHeight] (hanging down the back from the attach point), not [-wingHeight, 0], or it
        // grows upward past the head instead.
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-wingWidth, 0, 0, wingWidth, wingHeight, 0),
                PartPose.offsetAndRotation(-1F, 1F, 2.5F, 0, 0, 0));

        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, wingWidth, wingHeight, 0),
                PartPose.offsetAndRotation(1F, 1F, 2.5F, 0, 0, 0));

        return LayerDefinition.create(mesh, (int) (wingWidth * 2), (int) wingHeight);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        float rotX = 0F;
        float rotY = 0F;
        float rotZ = 0F;
        if (entity instanceof AbstractClientPlayer player)
        {
            rotX = player.elytraRotX;
            rotY = player.elytraRotY;
            rotZ = player.elytraRotZ;
        }

        rightWing.xRot = BASE_X + rotX;
        rightWing.yRot = -(BASE_Y + rotY);
        rightWing.zRot = -(BASE_Z + rotZ);

        leftWing.xRot = BASE_X + rotX;
        leftWing.yRot = BASE_Y + rotY;
        leftWing.zRot = BASE_Z + rotZ;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        rightWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
