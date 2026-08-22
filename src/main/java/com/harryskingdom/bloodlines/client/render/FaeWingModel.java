package com.harryskingdom.bloodlines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Stationary, fully-open butterfly wing silhouette for the Fae - a checkpoint build for silhouette approval only,
 * no folding/flapping yet (see FaeWingsLayer).
 * <p>
 * This deliberately does NOT use Minecraft's cuboid ModelPart/CubeListBuilder system: a cuboid always renders as
 * a rectangular box (even at 1-unit thickness, its side faces read as a slab), so no combination of thin boxes
 * can produce a curved, organic wing outline. Instead each wing is a flat, paper-thin, double-sided polygon
 * built by hand from a small ring of control points and smoothed into a curve with Catmull-Rom subdivision, then
 * triangulated as a fan from its centroid and pushed straight into the VertexConsumer - the geometry itself
 * defines the silhouette; the texture only supplies internal detail (see fae_wings.png).
 */
public class FaeWingModel
{
    // Control points for one closed, organic wing outline, right side, local space: origin = wing root
    // (attaches near the upper back), +X = outward away from the body, +Y = downward. Deliberately narrow near
    // the root and broad across the outer two-thirds, with a rounded top lobe and a curved, tapered tip.
    private static final float[][] UPPER_CONTROL = {
            {0.3F, -0.3F},
            {1.2F, -2.0F},
            {3.5F, -4.0F},
            {7.0F, -5.0F},
            {12.0F, -3.0F},
            {10.5F, 0.5F},
            {9.0F, 3.0F},
            {6.0F, 5.0F},
            {3.0F, 4.0F},
            {1.0F, 1.8F},
    };

    private static final float[][] LOWER_CONTROL = {
            {0.2F, -0.2F},
            {0.8F, -1.4F},
            {2.2F, -2.8F},
            {4.6F, -3.4F},
            {7.5F, -2.0F},
            {6.8F, 0.8F},
            {5.6F, 2.2F},
            {3.6F, 3.2F},
            {1.6F, 3.0F},
            {0.5F, 1.2F},
    };

    // Light smoothing only - just enough to round the control polygon's straight edges into curves without
    // rounding away the narrow root and the pointed tip the way heavier subdivision does.
    private static final int SUBDIVISIONS_PER_SEGMENT = 4;
    private static final float[][] UPPER_OUTLINE = smoothClosed(UPPER_CONTROL, SUBDIVISIONS_PER_SEGMENT);
    private static final float[][] LOWER_OUTLINE = smoothClosed(LOWER_CONTROL, SUBDIVISIONS_PER_SEGMENT);

    // Texture atlas regions (see fae_wings.png), UV in 0..1 texture space. Proportioned to roughly match each
    // outline's own bounding-box aspect ratio so the fill doesn't visibly stretch.
    private static final float[] UPPER_UV = {1 / 32F, 1 / 48F, 31 / 32F, 25 / 48F};
    private static final float[] LOWER_UV = {1 / 32F, 27 / 48F, 21 / 32F, 45 / 48F};

    // Pivots, right side (left mirrors X). Anchored on the upper back, not the shoulders. The lower wing sits
    // well below the upper wing's root so the two read as distinct lobes rather than one wing swallowing the
    // other (confirmed against a 2D silhouette preview before wiring this into the real render path).
    private static final float UPPER_PIVOT_X = 4.0F, UPPER_PIVOT_Y = -1.0F, UPPER_PIVOT_Z = 1.8F;
    private static final float LOWER_PIVOT_X = 4.0F, LOWER_PIVOT_Y = 4.0F, LOWER_PIVOT_Z = 2.3F;

    // Fixed fully-open stance for this checkpoint: upper sweeps outward and slightly up, lower outward and down.
    private static final float UPPER_OPEN_YAW = 0.16F;
    private static final float UPPER_OPEN_SWEEP = -0.2F;
    private static final float LOWER_OPEN_YAW = 0.1F;
    private static final float LOWER_OPEN_SWEEP = 0.2F;

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight)
    {
        renderSide(poseStack, buffer, packedLight, false);
        renderSide(poseStack, buffer, packedLight, true);
    }

    private void renderSide(PoseStack poseStack, VertexConsumer buffer, int packedLight, boolean left)
    {
        float sign = left ? -1.0F : 1.0F;

        poseStack.pushPose();
        poseStack.translate(sign * UPPER_PIVOT_X, UPPER_PIVOT_Y, UPPER_PIVOT_Z);
        poseStack.mulPose(Axis.YP.rotation(sign * UPPER_OPEN_YAW));
        poseStack.mulPose(Axis.ZP.rotation(sign * UPPER_OPEN_SWEEP));
        drawPanel(poseStack, buffer, packedLight, UPPER_OUTLINE, left, UPPER_UV);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(sign * LOWER_PIVOT_X, LOWER_PIVOT_Y, LOWER_PIVOT_Z);
        poseStack.mulPose(Axis.YP.rotation(sign * LOWER_OPEN_YAW));
        poseStack.mulPose(Axis.ZP.rotation(sign * LOWER_OPEN_SWEEP));
        drawPanel(poseStack, buffer, packedLight, LOWER_OUTLINE, left, LOWER_UV);
        poseStack.popPose();
    }

    private static void drawPanel(PoseStack poseStack, VertexConsumer buffer, int packedLight,
            float[][] outline, boolean left, float[] uvRect)
    {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float cx = 0, cy = 0;
        for (float[] p : outline)
        {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            cx += p[0];
            cy += p[1];
        }
        cx /= outline.length;
        cy /= outline.length;

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float cxLocal = left ? -cx : cx;
        float cu = uv(cx, minX, maxX, uvRect[0], uvRect[2]);
        float cv = uv(cy, minY, maxY, uvRect[1], uvRect[3]);

        int n = outline.length;
        for (int i = 0; i < n; i++)
        {
            float[] a = outline[i];
            float[] b = outline[(i + 1) % n];
            float ax = left ? -a[0] : a[0];
            float bx = left ? -b[0] : b[0];
            float au = uv(a[0], minX, maxX, uvRect[0], uvRect[2]);
            float av = uv(a[1], minY, maxY, uvRect[1], uvRect[3]);
            float bu = uv(b[0], minX, maxX, uvRect[0], uvRect[2]);
            float bv = uv(b[1], minY, maxY, uvRect[1], uvRect[3]);

            // Front face, then the same triangle wound the other way with the opposite normal, so the wing
            // reads correctly from both sides regardless of camera angle or the render type's cull state.
            vertex(buffer, matrix, normal, cxLocal, cy, cu, cv, packedLight, 0, 0, 1);
            vertex(buffer, matrix, normal, ax, a[1], au, av, packedLight, 0, 0, 1);
            vertex(buffer, matrix, normal, bx, b[1], bu, bv, packedLight, 0, 0, 1);
            vertex(buffer, matrix, normal, bx, b[1], bu, bv, packedLight, 0, 0, 1);

            vertex(buffer, matrix, normal, cxLocal, cy, cu, cv, packedLight, 0, 0, -1);
            vertex(buffer, matrix, normal, bx, b[1], bu, bv, packedLight, 0, 0, -1);
            vertex(buffer, matrix, normal, ax, a[1], au, av, packedLight, 0, 0, -1);
            vertex(buffer, matrix, normal, ax, a[1], au, av, packedLight, 0, 0, -1);
        }
    }

    private static float uv(float value, float min, float max, float uvMin, float uvMax)
    {
        return uvMin + (value - min) / (max - min) * (uvMax - uvMin);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
            float x, float y, float u, float v, int packedLight, float nx, float ny, float nz)
    {
        buffer.vertex(matrix, x, y, 0)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }

    /** Smooths a closed control polygon into a denser outline via Catmull-Rom subdivision. */
    private static float[][] smoothClosed(float[][] control, int subdivisions)
    {
        int n = control.length;
        float[][] result = new float[n * subdivisions][];
        int index = 0;
        for (int i = 0; i < n; i++)
        {
            float[] p0 = control[(i - 1 + n) % n];
            float[] p1 = control[i];
            float[] p2 = control[(i + 1) % n];
            float[] p3 = control[(i + 2) % n];
            for (int s = 0; s < subdivisions; s++)
            {
                float t = s / (float) subdivisions;
                result[index++] = catmullRom(p0, p1, p2, p3, t);
            }
        }
        return result;
    }

    private static float[] catmullRom(float[] p0, float[] p1, float[] p2, float[] p3, float t)
    {
        float t2 = t * t;
        float t3 = t2 * t;
        float x = 0.5F * (2 * p1[0] + (-p0[0] + p2[0]) * t
                + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3);
        float y = 0.5F * (2 * p1[1] + (-p0[1] + p2[1]) * t
                + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3);
        return new float[]{x, y};
    }
}
