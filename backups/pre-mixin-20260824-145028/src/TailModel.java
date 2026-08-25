package com.harryskingdom.bloodlines.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * A chain of tapering body segments ending in a fin, plus an optional second "fin2" part sharing the fin's
 * chain position - Beastkin uses both (two small tufts fanned apart, see createBeastkinLayer), Merfolk leaves
 * fin2 empty and uses one real fin, matching the real MerMod mod's own single-quad fin exactly (an earlier
 * attempt gave Merfolk two perpendicular fin plates for camera-angle robustness, but that read as "two fins"
 * and looked wrong - MerMod only ever uses one, so that's what we match now).
 * <p>
 * Merfolk's geometry - segment count, exact dimensions, and texOffs - is a direct port of MerMod's own
 * TailBuilder/TailLayerDefinitions.getDefault() source, not a re-derived approximation (see createMerfolkLayer
 * for the segment-by-segment mapping). MerMod's own "waist" segment duplicates the vanilla torso box (they
 * replace the whole lower body); Bloodlines only hides the legs (see TailLayer's swapLegsInWater), so our chain
 * starts one level lower, directly where MerMod's tail1 does - everything past that point is their real numbers.
 * <p>
 * Model space has +Y pointing down and +Z pointing toward the entity's back, matching every other vanilla
 * ModelPart. Rotating a segment around X curls it back/down (the static per-segment "droop"); rotating around
 * Z swings it side to side (the animated "wag", Beastkin only - see setupAnim's dolphinStyle branch for
 * Merfolk's real up/down swim wave instead).
 */
public class TailModel<T extends LivingEntity> extends EntityModel<T>
{
    private final ModelPart[] segments;
    private final ModelPart fin;
    private final ModelPart fin2;
    private final float[] droop;
    private final float finDroop;
    private final boolean dolphinStyle;

    public TailModel(ModelPart root, float[] droop, float finDroop, boolean dolphinStyle)
    {
        ModelPart[] parts = new ModelPart[droop.length];
        ModelPart current = root;
        for (int i = 0; i < droop.length; i++)
        {
            current = current.getChild("segment" + i);
            parts[i] = current;
        }
        this.segments = parts;
        this.fin = current.getChild("fin");
        this.fin2 = current.getChild("fin2");
        this.droop = droop;
        this.finDroop = finDroop;
        this.dolphinStyle = dolphinStyle;
    }

    /**
     * The tip fin slots get a small puffy tuft instead of staying empty - a low-risk nod to the real "tails4u"
     * pack's own "floof"/"floof2" tip decoration (a small pair of flat tufts fanned apart at the very end of
     * the tail), reusing the existing cross-plate fin/fin2 parts rather than adding new geometry slots.
     */
    public static LayerDefinition createBeastkinLayer()
    {
        return createLayer(64, 64,
                new Segment(CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0F, -1.5F, 3F, 5F, 3F), 5F),
                new Segment(CubeListBuilder.create().texOffs(0, 16).addBox(-1.25F, 0F, -1.25F, 2.5F, 5F, 2.5F), 5F),
                new Segment(CubeListBuilder.create().texOffs(0, 32).addBox(-1F, 0F, -1F, 2F, 4F, 2F), 3F))
                .fin(() -> CubeListBuilder.create().texOffs(0, 44).addBox(-1.5F, -0.5F, -1F, 3F, 2F, 1F),
                        () -> CubeListBuilder.create().texOffs(0, 44).addBox(-1.5F, -0.5F, -1F, 3F, 2F, 1F));
    }

    /**
     * A direct port of the real MerMod mod's own TailLayerDefinitions.getDefault() (traced from their actual
     * source, not re-derived): 7 tapering segments (tail1..tail7, 8 wide down to 5 wide) plus one big flat fin
     * (23 wide), using their exact dimensions and texOffs coordinates. MerMod's own "waist" segment duplicates
     * the vanilla torso box (they replace the whole lower body); we don't need that duplicate since Bloodlines
     * only hides the legs, so the chain starts one level lower, directly where MerMod's tail1 does - everything
     * past that point is their real numbers, not an approximation. merfolk_tail.png is their actual tail.png,
     * copied in unmodified, since matching their exact geometry means their texture lines up without repainting.
     */
    public static LayerDefinition createMerfolkLayer()
    {
        return createLayer(48, 64,
                new Segment(CubeListBuilder.create().texOffs(0, 0).addBox(-4F, 0F, -2F, 8F, 4F, 4F), 4F),
                new Segment(CubeListBuilder.create().texOffs(0, 8).addBox(-3.75F, 0F, -1.75F, 7.5F, 3F, 3.5F), 3F),
                new Segment(CubeListBuilder.create().texOffs(0, 15).addBox(-3.5F, 0F, -1.5F, 7F, 2F, 3F), 2F),
                new Segment(CubeListBuilder.create().texOffs(0, 20).addBox(-3.25F, 0F, -1.25F, 6.5F, 2F, 2.5F), 2F),
                new Segment(CubeListBuilder.create().texOffs(0, 25).addBox(-3F, 0F, -1F, 6F, 2F, 2F), 2F),
                new Segment(CubeListBuilder.create().texOffs(0, 29).addBox(-2.75F, 0F, -0.75F, 5.5F, 2F, 1.5F), 2F),
                new Segment(CubeListBuilder.create().texOffs(0, 33).addBox(-2.5F, 0F, -0.5F, 5F, 2F, 1F), 2F))
                .fin(() -> CubeListBuilder.create().texOffs(0, 40).addBox(-11.5F, 0F, 0F, 23F, 24F, 0F),
                        TailModel::emptyFin);
    }

    private static CubeListBuilder emptyFin()
    {
        return CubeListBuilder.create();
    }

    private record Segment(CubeListBuilder builder, float length) {}

    private static PendingLayer createLayer(int texWidth, int texHeight, Segment... segments)
    {
        return new PendingLayer(texWidth, texHeight, segments);
    }

    private record PendingLayer(int texWidth, int texHeight, Segment[] segments)
    {
        LayerDefinition fin(java.util.function.Supplier<CubeListBuilder> finBuilder)
        {
            return fin(finBuilder, TailModel::emptyFin);
        }

        LayerDefinition fin(java.util.function.Supplier<CubeListBuilder> finBuilder, java.util.function.Supplier<CubeListBuilder> fin2Builder)
        {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition current = mesh.getRoot();
            float pendingOffset = 0F;
            for (int i = 0; i < segments.length; i++)
            {
                current = current.addOrReplaceChild("segment" + i, segments[i].builder(), PartPose.offset(0F, pendingOffset, 0F));
                pendingOffset = segments[i].length();
            }
            current.addOrReplaceChild("fin", finBuilder.get(), PartPose.offset(0F, pendingOffset, 0F));
            current.addOrReplaceChild("fin2", fin2Builder.get(),
                    PartPose.offsetAndRotation(0F, pendingOffset, 0F, 0F, Mth.HALF_PI, 0F));
            return LayerDefinition.create(mesh, texWidth, texHeight);
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        int n = segments.length;

        if (dolphinStyle)
        {
            // A traveling wave down the tail (each segment's phase lagging the previous one), toned down hard
            // from MerMod's own real amplitude/fin-doubling - their numbers assume their own custom body-tilt
            // override during swimming, which Bloodlines doesn't have, so the same "small" angles compound with
            // vanilla's own swim-tilt rotation into a much bigger visible curl than they get. The phase lag
            // between adjacent segments (0.05 here, half MerMod's own 0.1) is what actually controls how far
            // adjacent segments' joints visibly separate during the animation - too much lag and neighboring
            // segments sit at meaningfully different angles at the same instant, opening a gap at the seam
            // between them (reported as "gaps in the tail"), independent of the overall wave amplitude.
            float pos = ageInTicks * 0.2F + limbSwing * 0.8F;
            for (int i = 0; i < n; i++)
            {
                float wave = Mth.sin(Mth.TWO_PI * (pos * 0.035F - 0.05F * i)) * (Mth.PI / 48F);
                segments[i].xRot = droop[i] + wave;
                segments[i].zRot = 0F;
            }
            fin.xRot = fin2.xRot = segments[n - 1].xRot;
            fin.zRot = fin2.zRot = 0F;
        }
        else
        {
            float idleSway = Mth.sin(ageInTicks * 0.1F) * 0.15F;
            float walkSway = Mth.sin(limbSwing * 0.6662F) * limbSwingAmount * 0.4F;
            float wag = idleSway + walkSway;

            for (int i = 0; i < n; i++)
            {
                segments[i].xRot = droop[i];
                segments[i].zRot = wag * ((i + 1F) / n);
            }
            fin.xRot = fin2.xRot = finDroop;
            fin.zRot = fin2.zRot = wag * 1.15F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha)
    {
        segments[0].render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
