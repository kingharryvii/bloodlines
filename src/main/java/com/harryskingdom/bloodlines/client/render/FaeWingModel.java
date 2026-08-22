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
 * Dedicated butterfly-style wing rig for the Fae, replacing vanilla's ElytraModel reuse (whose wing cubes are a
 * 2-wide x 20-tall strip - a narrow cape by construction, no texture can fix that).
 * <p>
 * Each side has an independent root bone (pivoting near the upper back) carrying two leaf parts: a large upper
 * wing (~65% of the surface area) and a smaller lower wing (~35%), overlapping slightly at the root. The root
 * bone handles the overall open/fold spread; the upper and lower parts carry their own additional rotation so
 * they can move with a slight timing offset during a flap.
 * <p>
 * Texture layout (see fae_wings.png, 32x32): the right-side upper wing template lives at (0,0)-(24,10), the
 * right-side lower wing template at (0,11)-(18,18). The left side reuses both regions mirrored, so only one
 * pair of wing shapes needs to be drawn.
 */
public class FaeWingModel
{
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "fae_wings"), "main");

    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 32;

    private final ModelPart rightWingRoot;
    private final ModelPart rightUpperWing;
    private final ModelPart rightLowerWing;
    private final ModelPart leftWingRoot;
    private final ModelPart leftUpperWing;
    private final ModelPart leftLowerWing;

    public FaeWingModel(ModelPart root)
    {
        this.rightWingRoot = root.getChild("right_wing_root");
        this.rightUpperWing = rightWingRoot.getChild("right_upper_wing");
        this.rightLowerWing = rightWingRoot.getChild("right_lower_wing");
        this.leftWingRoot = root.getChild("left_wing_root");
        this.leftUpperWing = leftWingRoot.getChild("left_upper_wing");
        this.leftLowerWing = leftWingRoot.getChild("left_lower_wing");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        PartDefinition rightRoot = parts.addOrReplaceChild("right_wing_root",
                CubeListBuilder.create(), PartPose.offset(4.5F, 0.0F, 2.0F));
        rightRoot.addOrReplaceChild("right_upper_wing",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -4.0F, -0.5F, 11.0F, 9.0F, 1.0F),
                PartPose.ZERO);
        rightRoot.addOrReplaceChild("right_lower_wing",
                CubeListBuilder.create().texOffs(0, 11).addBox(0.0F, 3.0F, -0.5F, 8.0F, 6.0F, 1.0F),
                PartPose.ZERO);

        PartDefinition leftRoot = parts.addOrReplaceChild("left_wing_root",
                CubeListBuilder.create(), PartPose.offset(-4.5F, 0.0F, 2.0F));
        leftRoot.addOrReplaceChild("left_upper_wing",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-11.0F, -4.0F, -0.5F, 11.0F, 9.0F, 1.0F),
                PartPose.ZERO);
        leftRoot.addOrReplaceChild("left_lower_wing",
                CubeListBuilder.create().texOffs(0, 11).mirror().addBox(-8.0F, 3.0F, -0.5F, 8.0F, 6.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /**
     * Poses every part for this frame. {@code openAmount} (0=folded, 1=fully spread) should already be smoothed
     * frame-to-frame by the caller. {@code flapping} enables the butterfly wingbeat layered on top of the open
     * pose; {@code flapPhase} is a continuously increasing angle (e.g. ageInTicks * speed) driving that beat.
     */
    public void setPose(float openAmount, boolean flapping, float flapPhase)
    {
        float rootYaw = lerp(openAmount, FOLDED_ROOT_YAW, OPEN_ROOT_YAW);
        float rootPitch = lerp(openAmount, FOLDED_ROOT_PITCH, OPEN_ROOT_PITCH);
        float upperPitch = lerp(openAmount, FOLDED_UPPER_PITCH, OPEN_UPPER_PITCH);
        float lowerPitch = lerp(openAmount, FOLDED_LOWER_PITCH, OPEN_LOWER_PITCH);

        float upperFlap = 0.0F;
        float lowerFlap = 0.0F;
        float upperRoll = 0.0F;
        float lowerRoll = 0.0F;
        if (flapping)
        {
            upperFlap = (float) Math.sin(flapPhase) * FLAP_AMPLITUDE_UPPER;
            lowerFlap = (float) Math.sin(flapPhase - FLAP_LAG) * FLAP_AMPLITUDE_LOWER;
            upperRoll = (float) Math.cos(flapPhase) * FLAP_ROLL_AMPLITUDE;
            lowerRoll = (float) Math.cos(flapPhase - FLAP_LAG) * FLAP_ROLL_AMPLITUDE;
        }

        rightWingRoot.yRot = rootYaw;
        rightWingRoot.xRot = rootPitch;
        rightUpperWing.xRot = upperPitch + upperFlap;
        rightUpperWing.zRot = upperRoll;
        rightLowerWing.xRot = lowerPitch + lowerFlap;
        rightLowerWing.zRot = lowerRoll;

        leftWingRoot.yRot = -rootYaw;
        leftWingRoot.xRot = rootPitch;
        leftUpperWing.xRot = upperPitch + upperFlap;
        leftUpperWing.zRot = -upperRoll;
        leftLowerWing.xRot = lowerPitch + lowerFlap;
        leftLowerWing.zRot = -lowerRoll;
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay)
    {
        rightWingRoot.render(poseStack, buffer, packedLight, packedOverlay);
        leftWingRoot.render(poseStack, buffer, packedLight, packedOverlay);
    }

    private static float lerp(float t, float from, float to)
    {
        return from + (to - from) * t;
    }

    // Folded: wings swept back and tucked close against the spine.
    private static final float FOLDED_ROOT_YAW = 1.1F;
    private static final float FOLDED_ROOT_PITCH = 0.05F;
    private static final float FOLDED_UPPER_PITCH = -0.2F;
    private static final float FOLDED_LOWER_PITCH = 0.3F;

    // Fully open: swept out to the side, upper wing angled up/out, lower wing angled slightly down.
    private static final float OPEN_ROOT_YAW = 0.15F;
    private static final float OPEN_ROOT_PITCH = -0.1F;
    private static final float OPEN_UPPER_PITCH = -0.5F;
    private static final float OPEN_LOWER_PITCH = 0.15F;

    // Flap: downstroke (positive) then a slightly offset, smaller recovery stroke on the lower wing.
    private static final float FLAP_AMPLITUDE_UPPER = 0.55F;
    private static final float FLAP_AMPLITUDE_LOWER = 0.4F;
    private static final float FLAP_ROLL_AMPLITUDE = 0.12F;
    private static final float FLAP_LAG = 0.7F;
}
