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
 * A small head-mounted accessory: either one centered piece (the halo - "right" left empty) or a mirrored pair
 * (horns, elf ears, cat ears). Positioning is entirely the baked PartPose from each createXxxLayer() factory -
 * these are small and rigid, unlike TailModel's chained segments, and only need to follow the head's own
 * rotation (handled by HeadAccessoryLayer via ModelPart#translateAndRotate) plus an optional slow idle spin
 * for the halo.
 */
public class HeadAccessoryModel<T extends LivingEntity> extends EntityModel<T>
{
    private final ModelPart left;
    private final ModelPart right;
    private final boolean spin;

    public HeadAccessoryModel(ModelPart root, boolean spin)
    {
        this.left = root.getChild("left");
        this.right = root.getChild("right");
        this.spin = spin;
    }

    /**
     * The real reference (Bloodlines Textures/1.13.x Angel Halo (32)) is a smoothed ring built from hundreds
     * of tiny segments - its model JSON is a single ~340KB minified line, not practical to hand-port safely.
     * This is still real 3D ring geometry rather than a texture trick (a flat plate with a punched alpha hole
     * was the earlier, broken attempt), just built from 16 overlapping cubes instead of hundreds of tiny ones.
     * The previous 12-cube version left visible gaps between segments (read as "floating cubes"); this uses
     * more segments (16) AND makes each one wider (2.2, versus the ~1.95 spacing between segment centers) so
     * neighboring cubes overlap and the ring reads as continuous. halo.png was also the actual bug behind
     * "should be golden, not white" - its highlight band covered the entire tiny UV footprint these cubes
     * sample from, so every cube showed the pale highlight color instead of the base gold underneath.
     */
    public static LayerDefinition createHaloLayer()
    {
        CubeListBuilder halo = CubeListBuilder.create().texOffs(0, 0)
                .addBox(3.9F, -0.5F, -1.1F, 2.2F, 1F, 2.2F)
                .addBox(3.52F, -0.5F, 0.81F, 2.2F, 1F, 2.2F)
                .addBox(2.44F, -0.5F, 2.44F, 2.2F, 1F, 2.2F)
                .addBox(0.81F, -0.5F, 3.52F, 2.2F, 1F, 2.2F)
                .addBox(-1.1F, -0.5F, 3.9F, 2.2F, 1F, 2.2F)
                .addBox(-3.01F, -0.5F, 3.52F, 2.2F, 1F, 2.2F)
                .addBox(-4.64F, -0.5F, 2.44F, 2.2F, 1F, 2.2F)
                .addBox(-5.72F, -0.5F, 0.81F, 2.2F, 1F, 2.2F)
                .addBox(-6.1F, -0.5F, -1.1F, 2.2F, 1F, 2.2F)
                .addBox(-5.72F, -0.5F, -3.01F, 2.2F, 1F, 2.2F)
                .addBox(-4.64F, -0.5F, -4.64F, 2.2F, 1F, 2.2F)
                .addBox(-3.01F, -0.5F, -5.72F, 2.2F, 1F, 2.2F)
                .addBox(-1.1F, -0.5F, -6.1F, 2.2F, 1F, 2.2F)
                .addBox(0.81F, -0.5F, -5.72F, 2.2F, 1F, 2.2F)
                .addBox(2.44F, -0.5F, -4.64F, 2.2F, 1F, 2.2F)
                .addBox(3.52F, -0.5F, -3.01F, 2.2F, 1F, 2.2F);
        return createLayer(halo, PartPose.offset(0F, -10F, 0F), CubeListBuilder.create(), PartPose.ZERO);
    }

    /**
     * Ported from the real "whimsy-horns-and-antlers" pack's demon5.json (the user's own reference file,
     * Bloodlines Textures/whimsy-horns-and-antlers-v1) - every element in that file uses rotation angle 0, so
     * the curved horn shape comes entirely from each small segment's own position, not from any per-box
     * rotation. That meant the box positions could be transformed directly: item-model space is Y-up in a
     * 0-16 cube (item's own local origin near the model's overall center), so each box's start.y becomes
     * -4 - to.y here (Y-down, plus a shift so the curve's wide base sits flush against the head top at y=-8)
     * and x/z are recentered on the right horn's own rotation.origin, [10, 8, 4] from that file. The left horn
     * is the same 6 segments mirrored across x.
     */
    public static LayerDefinition createHornsLayer()
    {
        CubeListBuilder hornRight = CubeListBuilder.create().texOffs(0, 0)
                .addBox(1F, -14.75F, -3.75F, 1F, 1F, 1F)
                .addBox(1.5F, -14.25F, -3.25F, 1F, 2F, 1F)
                .addBox(2F, -12.75F, -2.75F, 1F, 2F, 1F)
                .addBox(2.5F, -12.25F, -1.75F, 1F, 3F, 2F)
                .addBox(3F, -11.5F, 0.25F, 1F, 3F, 2F)
                .addBox(1.75F, -11F, 1.75F, 2F, 3F, 3F);
        CubeListBuilder hornLeft = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2F, -14.75F, -3.75F, 1F, 1F, 1F)
                .addBox(-2.5F, -14.25F, -3.25F, 1F, 2F, 1F)
                .addBox(-3F, -12.75F, -2.75F, 1F, 2F, 1F)
                .addBox(-3.5F, -12.25F, -1.75F, 1F, 3F, 2F)
                .addBox(-4F, -11.5F, 0.25F, 1F, 3F, 2F)
                .addBox(-3.75F, -11F, 1.75F, 2F, 3F, 3F);
        return createLayer(hornLeft, PartPose.ZERO, hornRight, PartPose.ZERO);
    }

    /**
     * A real port of the pointyears pack's own darkElf.json (Bloodlines Textures/pointyears/normal), all 17
     * boxes per ear, not an approximation. That file's whole ear rotates 22.5 degrees around Y at a pivot far
     * from the geometry itself ([-1,0,0], while the boxes sit around x=10) - computed by rotating each box's
     * corners around that pivot (PowerShell, not by hand, to avoid arithmetic mistakes across 34 boxes), then
     * recentered on the file's own group origin [8,8,8] and scaled by 0.55 to fit a player head. That rotation
     * also swaps which axis ends up being "left-right" versus "front-back" in the final frame - the ear's own
     * from/to Z became this model's X once rotated, not its from/to X - using the raw axes unswapped is what
     * produced the earlier "floating square that isn't even attached" result.
     */
    public static LayerDefinition createElfEarsLayer()
    {
        CubeListBuilder earRight = CubeListBuilder.create().texOffs(0, 0)
                .addBox(2.59F, 0.55F, -1.465F, 0.512F, 0.55F, 0.422F)
                .addBox(2.488F, 1.1F, -1.465F, 0.613F, 0.55F, 0.465F)
                .addBox(2.997F, 0.275F, -1.676F, 0.613F, 0.275F, 0.465F)
                .addBox(3.018F, 1.65F, -1.625F, 0.613F, 0.495F, 0.465F)
                .addBox(3.505F, 1.65F, -1.886F, 0.613F, 0.330F, 0.465F)
                .addBox(4.013F, 1.1F, -2.097F, 0.613F, 0.55F, 0.465F)
                .addBox(4.013F, 0.55F, -2.097F, 0.613F, 0.55F, 0.465F)
                .addBox(4.013F, 0.110F, -2.097F, 0.613F, 0.440F, 0.465F)
                .addBox(3.505F, 0.220F, -1.886F, 0.613F, 0.330F, 0.465F)
                .addBox(3.505F, 0.55F, -1.886F, 0.592F, 0.55F, 0.414F)
                .addBox(3.505F, 1.1F, -1.886F, 0.592F, 0.55F, 0.414F)
                .addBox(2.997F, 1.1F, -1.676F, 0.571F, 0.55F, 0.363F)
                .addBox(2.997F, 0.55F, -1.676F, 0.634F, 0.55F, 0.515F)
                .addBox(4.563F, 0F, -2.206F, 0.613F, 0.55F, 0.465F)
                .addBox(4.563F, 0.55F, -2.206F, 0.592F, 0.55F, 0.414F)
                .addBox(5.113F, 0F, -2.314F, 0.613F, 0.55F, 0.465F)
                .addBox(5.621F, -0.055F, -2.525F, 0.613F, 0.330F, 0.465F);
        // A clean X-mirror of earRight (newX = -(x + width), y/z/dimensions unchanged), not an independent
        // re-port from the source file. The real darkElf.json rotates the right ear +22.5 degrees and the left
        // ear -22.5 degrees around the SAME pivot (not a mirrored one) - porting each side's 17 boxes
        // independently by baking that rotation into per-box corner coordinates is exactly the kind of subtle,
        // easy-to-get-wrong-on-one-side math that produced the original bug (confirmed in testing: right ear
        // correct, left ear not). Since both ears attach to a perfectly symmetric player head at the same
        // PartPose offset, a plain mirror of the known-good right ear is guaranteed symmetric by construction,
        // instead of trusting a second independent derivation to come out equally correct.
        CubeListBuilder earLeft = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.102F, 0.55F, -1.465F, 0.512F, 0.55F, 0.422F)
                .addBox(-3.101F, 1.1F, -1.465F, 0.613F, 0.55F, 0.465F)
                .addBox(-3.61F, 0.275F, -1.676F, 0.613F, 0.275F, 0.465F)
                .addBox(-3.631F, 1.65F, -1.625F, 0.613F, 0.495F, 0.465F)
                .addBox(-4.118F, 1.65F, -1.886F, 0.613F, 0.330F, 0.465F)
                .addBox(-4.626F, 1.1F, -2.097F, 0.613F, 0.55F, 0.465F)
                .addBox(-4.626F, 0.55F, -2.097F, 0.613F, 0.55F, 0.465F)
                .addBox(-4.626F, 0.110F, -2.097F, 0.613F, 0.440F, 0.465F)
                .addBox(-4.118F, 0.220F, -1.886F, 0.613F, 0.330F, 0.465F)
                .addBox(-4.097F, 0.55F, -1.886F, 0.592F, 0.55F, 0.414F)
                .addBox(-4.097F, 1.1F, -1.886F, 0.592F, 0.55F, 0.414F)
                .addBox(-3.568F, 1.1F, -1.676F, 0.571F, 0.55F, 0.363F)
                .addBox(-3.631F, 0.55F, -1.676F, 0.634F, 0.55F, 0.515F)
                .addBox(-5.176F, 0F, -2.206F, 0.613F, 0.55F, 0.465F)
                .addBox(-5.155F, 0.55F, -2.206F, 0.592F, 0.55F, 0.414F)
                .addBox(-5.726F, 0F, -2.314F, 0.613F, 0.55F, 0.465F)
                .addBox(-6.234F, -0.055F, -2.525F, 0.613F, 0.330F, 0.465F);
        return createLayer(earLeft, PartPose.offset(0F, -5.5F, 0F), earRight, PartPose.offset(0F, -5.5F, 0F));
    }

    /**
     * A real port of the pointyears pack's own animal_ears/cat/normal/brownCat.json - all 45 boxes per ear.
     * Every box in that file uses rotation angle 0 (unlike the elf ears), so no rotation math was needed, just
     * a direct recenter on the file's group origin [8,8,8] and a 0.6 scale to fit a player head.
     */
    public static LayerDefinition createCatEarsLayer()
    {
        CubeListBuilder earRight = CubeListBuilder.create().texOffs(0, 0)
                .addBox(3.308F, 3.968F, 0.788F, 0.472F, 0.472F, 0.472F)
                .addBox(3.150F, 3.495F, 0.788F, 0.472F, 0.472F, 0.472F)
                .addBox(3.150F, 3.117F, 0.693F, 0.378F, 0.378F, 0.378F)
                .addBox(3.087F, 2.739F, 0.567F, 0.378F, 0.378F, 0.378F)
                .addBox(3.024F, 2.424F, 0.504F, 0.315F, 0.315F, 0.315F)
                .addBox(3.024F, 2.109F, 0.441F, 0.315F, 0.315F, 0.315F)
                .addBox(2.804F, 1.794F, 0.410F, 0.315F, 0.315F, 0.315F)
                .addBox(2.583F, 1.479F, 0.252F, 0.315F, 0.315F, 0.315F)
                .addBox(2.992F, 3.968F, 1.26F, 0.472F, 0.472F, 0.472F)
                .addBox(2.835F, 3.496F, 1.102F, 0.472F, 0.471F, 0.472F)
                .addBox(2.866F, 3.117F, 1.071F, 0.378F, 0.378F, 0.378F)
                .addBox(2.866F, 2.739F, 0.945F, 0.378F, 0.378F, 0.378F)
                .addBox(2.804F, 2.424F, 0.819F, 0.315F, 0.315F, 0.315F)
                .addBox(2.804F, 2.110F, 0.724F, 0.315F, 0.314F, 0.315F)
                .addBox(2.678F, 1.794F, 0.724F, 0.315F, 0.315F, 0.158F)
                .addBox(2.553F, 1.542F, 0.567F, 0.314F, 0.315F, 0.315F)
                .addBox(2.52F, 3.968F, 1.575F, 0.472F, 0.472F, 0.472F)
                .addBox(2.52F, 3.495F, 1.418F, 0.472F, 0.472F, 0.472F)
                .addBox(2.52F, 3.117F, 1.354F, 0.378F, 0.378F, 0.378F)
                .addBox(2.52F, 2.740F, 1.197F, 0.378F, 0.377F, 0.378F)
                .addBox(2.552F, 2.424F, 1.134F, 0.315F, 0.315F, 0.315F)
                .addBox(2.552F, 2.109F, 0.976F, 0.315F, 0.315F, 0.315F)
                .addBox(2.552F, 1.794F, 0.788F, 0.315F, 0.315F, 0.315F)
                .addBox(2.048F, 3.968F, 1.26F, 0.472F, 0.472F, 0.472F)
                .addBox(2.048F, 3.495F, 1.102F, 0.472F, 0.472F, 0.472F)
                .addBox(2.205F, 3.117F, 1.008F, 0.378F, 0.378F, 0.378F)
                .addBox(2.236F, 2.739F, 0.945F, 0.378F, 0.378F, 0.378F)
                .addBox(2.268F, 2.424F, 0.819F, 0.315F, 0.315F, 0.315F)
                .addBox(2.268F, 2.109F, 0.724F, 0.315F, 0.315F, 0.315F)
                .addBox(1.575F, 3.968F, 0.945F, 0.472F, 0.472F, 0.472F)
                .addBox(1.26F, 3.968F, 0.472F, 0.472F, 0.472F, 0.472F)
                .addBox(1.732F, 3.495F, 0.630F, 0.472F, 0.472F, 0.472F)
                .addBox(1.858F, 3.117F, 0.630F, 0.378F, 0.378F, 0.378F)
                .addBox(1.890F, 2.740F, 0.567F, 0.378F, 0.377F, 0.378F)
                .addBox(1.953F, 2.424F, 0.504F, 0.315F, 0.315F, 0.315F)
                .addBox(2.016F, 2.109F, 0.472F, 0.315F, 0.315F, 0.315F)
                .addBox(2.236F, 1.794F, 0.504F, 0.315F, 0.315F, 0.315F)
                .addBox(1.102F, 3.968F, 0F, 0.472F, 0.472F, 0.472F)
                .addBox(1.418F, 3.495F, 0.158F, 0.472F, 0.472F, 0.472F)
                .addBox(1.512F, 3.117F, 0.252F, 0.378F, 0.378F, 0.378F)
                .addBox(1.575F, 2.739F, 0.220F, 0.378F, 0.378F, 0.378F)
                .addBox(1.701F, 2.424F, 0.189F, 0.315F, 0.315F, 0.315F)
                .addBox(1.827F, 2.109F, 0.158F, 0.315F, 0.315F, 0.315F)
                .addBox(2.016F, 1.794F, 0.189F, 0.315F, 0.315F, 0.315F)
                .addBox(2.269F, 1.542F, 0.252F, 0.314F, 0.315F, 0.315F);
        CubeListBuilder earLeft = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.78F, 3.968F, 0.788F, 0.472F, 0.472F, 0.472F)
                .addBox(-3.622F, 3.495F, 0.788F, 0.472F, 0.472F, 0.472F)
                .addBox(-3.528F, 3.117F, 0.693F, 0.378F, 0.378F, 0.378F)
                .addBox(-3.465F, 2.739F, 0.567F, 0.378F, 0.378F, 0.378F)
                .addBox(-3.339F, 2.424F, 0.504F, 0.315F, 0.315F, 0.315F)
                .addBox(-3.339F, 2.109F, 0.441F, 0.315F, 0.315F, 0.315F)
                .addBox(-3.118F, 1.794F, 0.410F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.898F, 1.479F, 0.252F, 0.315F, 0.315F, 0.315F)
                .addBox(-3.465F, 3.968F, 1.26F, 0.472F, 0.472F, 0.472F)
                .addBox(-3.308F, 3.496F, 1.102F, 0.472F, 0.471F, 0.472F)
                .addBox(-3.244F, 3.117F, 1.071F, 0.378F, 0.378F, 0.378F)
                .addBox(-3.244F, 2.739F, 0.945F, 0.378F, 0.378F, 0.378F)
                .addBox(-3.118F, 2.424F, 0.819F, 0.315F, 0.315F, 0.315F)
                .addBox(-3.118F, 2.110F, 0.724F, 0.315F, 0.314F, 0.315F)
                .addBox(-2.992F, 1.794F, 0.724F, 0.315F, 0.315F, 0.158F)
                .addBox(-2.866F, 1.542F, 0.567F, 0.314F, 0.315F, 0.315F)
                .addBox(-2.992F, 3.968F, 1.575F, 0.472F, 0.472F, 0.472F)
                .addBox(-2.992F, 3.495F, 1.418F, 0.472F, 0.472F, 0.472F)
                .addBox(-2.898F, 3.117F, 1.354F, 0.378F, 0.378F, 0.378F)
                .addBox(-2.898F, 2.740F, 1.197F, 0.378F, 0.377F, 0.378F)
                .addBox(-2.866F, 2.424F, 1.134F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.866F, 2.109F, 0.976F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.866F, 1.794F, 0.788F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.52F, 3.968F, 1.26F, 0.472F, 0.472F, 0.472F)
                .addBox(-2.52F, 3.495F, 1.102F, 0.472F, 0.472F, 0.472F)
                .addBox(-2.583F, 3.117F, 1.008F, 0.378F, 0.378F, 0.378F)
                .addBox(-2.614F, 2.739F, 0.945F, 0.378F, 0.378F, 0.378F)
                .addBox(-2.583F, 2.424F, 0.819F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.583F, 2.109F, 0.724F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.048F, 3.968F, 0.945F, 0.472F, 0.472F, 0.472F)
                .addBox(-1.732F, 3.968F, 0.472F, 0.472F, 0.472F, 0.472F)
                .addBox(-2.205F, 3.495F, 0.630F, 0.472F, 0.472F, 0.472F)
                .addBox(-2.236F, 3.117F, 0.630F, 0.378F, 0.378F, 0.378F)
                .addBox(-2.268F, 2.740F, 0.567F, 0.378F, 0.377F, 0.378F)
                .addBox(-2.268F, 2.424F, 0.504F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.331F, 2.109F, 0.472F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.552F, 1.794F, 0.504F, 0.315F, 0.315F, 0.315F)
                .addBox(-1.575F, 3.968F, 0F, 0.472F, 0.472F, 0.472F)
                .addBox(-1.890F, 3.495F, 0.158F, 0.472F, 0.472F, 0.472F)
                .addBox(-1.890F, 3.117F, 0.252F, 0.378F, 0.378F, 0.378F)
                .addBox(-1.953F, 2.739F, 0.220F, 0.378F, 0.378F, 0.378F)
                .addBox(-2.016F, 2.424F, 0.189F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.142F, 2.109F, 0.158F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.331F, 1.794F, 0.189F, 0.315F, 0.315F, 0.315F)
                .addBox(-2.583F, 1.542F, 0.252F, 0.314F, 0.315F, 0.315F);
        // -13F sat the ears far enough above the head that they read as floating rather than attached; -11.5F
        // brings them down closer to the scalp without burying their base back inside the head box.
        return createLayer(earLeft, PartPose.offset(0F, -11.5F, 0F), earRight, PartPose.offset(0F, -11.5F, 0F));
    }

    private static LayerDefinition createLayer(CubeListBuilder left, PartPose leftPose, CubeListBuilder right, PartPose rightPose)
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left", left, leftPose);
        root.addOrReplaceChild("right", right, rightPose);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (spin)
        {
            left.yRot = ageInTicks * 0.03F;
            left.y = -10F + Mth.sin(ageInTicks * 0.05F) * 0.4F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha)
    {
        left.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        right.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
