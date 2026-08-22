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
 * Articulated butterfly-style wing rig for the Fae. Each side is two independent joint chains anchored on the
 * upper back: upper wing (base -> mid -> tip, the dominant section) and lower wing (base -> tip, smaller),
 * exactly mirroring the Bloodlines Fae wing blueprint's five named parts. Each joint only carries its own
 * rotation offset from its parent, so a chain bends like a multi-segment arm rather than swinging as one flat
 * plane - that's what gives the silhouette its curve, since the boxes themselves stay rectangular.
 * <p>
 * The "outward" rest direction for every part is +X (right side) / -X (left side, mirrored). Rotating a chain
 * around Z sweeps it between pointing sideways (open) and pointing straight down the spine (folded), which is
 * the axis {@link FaeWingsLayer} drives every frame; rotating around X (only exposed on the base joints, since
 * children inherit it) tilts the whole chain forward/back for a bit of depth to the motion.
 */
public class FaeWingModel
{
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "fae_wings"), "main");

    private static final int TEXTURE_WIDTH = 16;
    private static final int TEXTURE_HEIGHT = 40;

    private final ModelPart rightUpperBase;
    private final ModelPart rightUpperMid;
    private final ModelPart rightUpperTip;
    private final ModelPart rightLowerBase;
    private final ModelPart rightLowerTip;

    private final ModelPart leftUpperBase;
    private final ModelPart leftUpperMid;
    private final ModelPart leftUpperTip;
    private final ModelPart leftLowerBase;
    private final ModelPart leftLowerTip;

    public FaeWingModel(ModelPart root)
    {
        this.rightUpperBase = root.getChild("right_upper_base");
        this.rightUpperMid = rightUpperBase.getChild("right_upper_mid");
        this.rightUpperTip = rightUpperMid.getChild("right_upper_tip");
        this.rightLowerBase = root.getChild("right_lower_base");
        this.rightLowerTip = rightLowerBase.getChild("right_lower_tip");

        this.leftUpperBase = root.getChild("left_upper_base");
        this.leftUpperMid = leftUpperBase.getChild("left_upper_mid");
        this.leftUpperTip = leftUpperMid.getChild("left_upper_tip");
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
                box(left, 5, 7).texOffs(0, 0),
                PartPose.offset(sign * 4.0F, -1.0F, 1.8F));
        PartDefinition upperMid = upperBase.addOrReplaceChild(side + "_upper_mid",
                box(left, 5, 6).texOffs(0, 9),
                PartPose.offset(sign * 5.0F, 0.0F, 0.0F));
        upperMid.addOrReplaceChild(side + "_upper_tip",
                box(left, 4, 4).texOffs(0, 17),
                PartPose.offset(sign * 5.0F, 0.0F, 0.0F));

        PartDefinition lowerBase = parts.addOrReplaceChild(side + "_lower_base",
                box(left, 4, 5).texOffs(0, 23),
                PartPose.offset(sign * 4.0F, 1.2F, 2.3F));
        lowerBase.addOrReplaceChild(side + "_lower_tip",
                box(left, 3, 4).texOffs(0, 30),
                PartPose.offset(sign * 4.0F, 0.0F, 0.0F));
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
     * Poses one side. {@code sign} must be +1 for the right side and -1 for the left (the Z-swing and roll axes
     * are mirrored between sides; the pitch axis is not). Angles are in radians.
     */
    private void applyPose(boolean left,
            float upperBaseZ, float upperMidZ, float upperTipZ,
            float lowerBaseZ, float lowerTipZ, float pitch)
    {
        float sign = left ? -1.0F : 1.0F;
        ModelPart upperBase = left ? leftUpperBase : rightUpperBase;
        ModelPart upperMid = left ? leftUpperMid : rightUpperMid;
        ModelPart upperTip = left ? leftUpperTip : rightUpperTip;
        ModelPart lowerBase = left ? leftLowerBase : rightLowerBase;
        ModelPart lowerTip = left ? leftLowerTip : rightLowerTip;

        upperBase.zRot = sign * upperBaseZ;
        upperBase.xRot = pitch;
        upperMid.zRot = sign * upperMidZ;
        upperTip.zRot = sign * upperTipZ;

        lowerBase.zRot = sign * lowerBaseZ;
        lowerBase.xRot = pitch;
        lowerTip.zRot = sign * lowerTipZ;
    }

    public void setPose(float upperBaseZ, float upperMidZ, float upperTipZ,
            float lowerBaseZ, float lowerTipZ, float pitch)
    {
        applyPose(false, upperBaseZ, upperMidZ, upperTipZ, lowerBaseZ, lowerTipZ, pitch);
        applyPose(true, upperBaseZ, upperMidZ, upperTipZ, lowerBaseZ, lowerTipZ, pitch);
    }

    /** Lower wing renders first so the upper wing naturally stacks in front of it, per the fold reference. */
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay)
    {
        rightLowerBase.render(poseStack, buffer, packedLight, packedOverlay);
        leftLowerBase.render(poseStack, buffer, packedLight, packedOverlay);
        rightUpperBase.render(poseStack, buffer, packedLight, packedOverlay);
        leftUpperBase.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
