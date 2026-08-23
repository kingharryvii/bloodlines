package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Seraph's four-wing rig: large upper wings (base -> tip, the dominant pair) and smaller lower wings (base ->
 * tip), each side independent, each pair a 2-joint chain so it can bend mid-flap rather than swinging as one
 * rigid panel. Built from plain ModelPart cuboids (the same proven approach vanilla's own elytra uses, and that
 * Fae's wings ultimately settled on) rather than custom mesh geometry - feathered angel wings read fine as thin
 * geometric panels, unlike the organic butterfly silhouette Fae needed, so there's no reason to reach for
 * anything more complex here.
 * <p>
 * All animation (fold/open/flap) lives in SeraphWingsLayer, which reads flight state from
 * SeraphFlightController/SeraphFlapTracker - this class only owns geometry and pose application.
 */
public class SeraphWingModel
{
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "seraph_wings"), "main");

    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 40;

    private final ModelPart rightUpperBase;
    private final ModelPart rightUpperTip;
    private final ModelPart rightLowerBase;
    private final ModelPart rightLowerTip;

    private final ModelPart leftUpperBase;
    private final ModelPart leftUpperTip;
    private final ModelPart leftLowerBase;
    private final ModelPart leftLowerTip;

    public SeraphWingModel(ModelPart root)
    {
        this.rightUpperBase = root.getChild("right_upper_base");
        this.rightUpperTip = rightUpperBase.getChild("right_upper_tip");
        this.rightLowerBase = root.getChild("right_lower_base");
        this.rightLowerTip = rightLowerBase.getChild("right_lower_tip");

        this.leftUpperBase = root.getChild("left_upper_base");
        this.leftUpperTip = leftUpperBase.getChild("left_upper_tip");
        this.leftLowerBase = root.getChild("left_lower_base");
        this.leftLowerTip = leftLowerBase.getChild("left_lower_tip");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        buildSide(parts, false);
        buildSide(parts, true);

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private static void buildSide(PartDefinition parts, boolean left)
    {
        String side = left ? "left" : "right";
        float sign = left ? -1.0F : 1.0F;

        PartDefinition upperBase = parts.addOrReplaceChild(side + "_upper_base",
                box(left, 13, 10).texOffs(0, 0),
                PartPose.offset(sign * 4.5F, -1.0F, 1.8F));
        upperBase.addOrReplaceChild(side + "_upper_tip",
                box(left, 10, 8).texOffs(0, 12),
                PartPose.offset(sign * 13.0F, 0.0F, 0.0F));

        PartDefinition lowerBase = parts.addOrReplaceChild(side + "_lower_base",
                box(left, 8, 7).texOffs(0, 22),
                PartPose.offset(sign * 4.5F, 2.5F, 2.4F));
        lowerBase.addOrReplaceChild(side + "_lower_tip",
                box(left, 6, 5).texOffs(0, 31),
                PartPose.offset(sign * 8.0F, 0.0F, 0.0F));
    }

    /** A thin (1-deep) box of the given width/height, centered vertically and depth-wise on its own pivot. */
    private static CubeListBuilder box(boolean left, float width, float height)
    {
        CubeListBuilder builder = CubeListBuilder.create();
        if (left)
            builder.mirror();
        float originX = left ? -width : 0.0F;
        return builder.addBox(originX, -height * 0.5F, -0.5F, width, height, 1.0F);
    }

    /**
     * Poses one side. {@code sign} must be +1 for the right side and -1 for the left (the Z-swing axis mirrors
     * between sides). Angles are in radians.
     */
    private void applyPose(boolean left, float upperBaseZ, float upperTipZ, float lowerBaseZ, float lowerTipZ, float pitch)
    {
        float sign = left ? -1.0F : 1.0F;
        ModelPart upperBase = left ? leftUpperBase : rightUpperBase;
        ModelPart upperTip = left ? leftUpperTip : rightUpperTip;
        ModelPart lowerBase = left ? leftLowerBase : rightLowerBase;
        ModelPart lowerTip = left ? leftLowerTip : rightLowerTip;

        upperBase.zRot = sign * upperBaseZ;
        upperBase.xRot = pitch;
        upperTip.zRot = sign * upperTipZ;

        lowerBase.zRot = sign * lowerBaseZ;
        lowerBase.xRot = pitch;
        lowerTip.zRot = sign * lowerTipZ;
    }

    public void setPose(float upperBaseZ, float upperTipZ, float lowerBaseZ, float lowerTipZ, float pitch)
    {
        applyPose(false, upperBaseZ, upperTipZ, lowerBaseZ, lowerTipZ, pitch);
        applyPose(true, upperBaseZ, upperTipZ, lowerBaseZ, lowerTipZ, pitch);
    }

    /** Lower wings render first so the upper wings naturally stack in front when folded. */
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay)
    {
        rightLowerBase.render(poseStack, buffer, packedLight, packedOverlay);
        leftLowerBase.render(poseStack, buffer, packedLight, packedOverlay);
        rightUpperBase.render(poseStack, buffer, packedLight, packedOverlay);
        leftUpperBase.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
