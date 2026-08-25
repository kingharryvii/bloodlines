package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.model.TailModel;
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

/**
 * Renders a race-specific tail (Beastkin's fur tail, Merfolk's fish tail). Angelkin and Demonkin are
 * deliberately never given a layer instance here - they already have wings, per the user's own call not to
 * stack a second cosmetic on top of those two races.
 * <p>
 * Geometry is genuinely new (see TailModel), since unlike wings there's no vanilla tail-shaped model to reuse.
 * Textures are simple placeholder color sheets pending real art, matching fae_wings.png's own interim status.
 * <p>
 * When swapLegsInWater is set (Merfolk only - traced from the real MerMod mod's own gate, "Mermod.java"'s
 * getRenderedTailStyle: {@code style != null && (player.isInWater() || style.permanent())}), the tail only
 * shows while the entity is actually touching water. Entity#isInWater() is plain client-computed fluid/AABB
 * overlap (same call vanilla's own swim animation already relies on for every rendered player, local or
 * remote), so no capability sync is needed for this - the race check alone still goes through ClientRaceCache
 * like every other cosmetic in this mod. The matching leg-hide lives in BloodlinesClientEvents#onRenderPlayerPre,
 * not here - RenderLayers only run after the base PlayerModel has already drawn for the frame, so a mutation
 * made from this class's render() always arrives one frame late.
 * <p>
 * Positioning is a single plain translate on the incoming PoseStack - no attempt to cancel/reconstruct vanilla's
 * own swim-tilt rotation. An earlier version tried exactly that (decompiled PlayerRenderer#setupRotations and
 * mathematically inverted its rotate+translate chain to re-pivot around the hip instead of the feet), and while
 * the derivation was sound for a level swim, it visibly broke at the large XRot values a real dive/sprint
 * produces - the tail ended up swinging up near the head, which is a worse regression than the plain version's
 * original narrower issue (detaching specifically while sprint-swimming with sneak held, i.e. diving steeply).
 * The plain translate is simpler, was independently confirmed to look right for ordinary swimming and standing,
 * and only has a problem in that one steep-dive case - not worth trading away the common case to chase it.
 */
public class TailLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M>
{
    /** Vanilla's own torso (HumanoidModel#body) box height, in the model's pixel-scale local units. */
    private static final float TORSO_HEIGHT = 12F;

    private final TailModel<T> model;
    private final ResourceLocation texture;
    private final Race race;
    private final float heightFraction;
    private final float backOffset;
    private final boolean swapLegsInWater;
    private final boolean anchorToBody;

    public TailLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet, ModelLayerLocation layerLocation,
            String textureName, Race race, float[] droop, float finDroop, boolean dolphinStyle,
            float heightFraction, float backOffset, boolean swapLegsInWater, boolean anchorToBody)
    {
        super(renderer);
        this.model = new TailModel<>(modelSet.bakeLayer(layerLocation), droop, finDroop, dolphinStyle);
        this.texture = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/" + textureName + ".png");
        this.race = race;
        this.heightFraction = heightFraction;
        this.backOffset = backOffset;
        this.swapLegsInWater = swapLegsInWater;
        this.anchorToBody = anchorToBody;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        boolean raceMatches = ClientRaceCache.get(entity.getId()) == race;
        boolean showTail = raceMatches && (!swapLegsInWater || entity.isInWater());

        if (!showTail)
            return;

        poseStack.pushPose();

        if (anchorToBody)
        {
            getParentModel().body.translateAndRotate(poseStack);
            // TORSO_HEIGHT is a pixel-unit offset (matching PartPose/ModelPart's own convention), but this is a
            // raw PoseStack.translate() call, not a ModelPart#translateAndRotate() - PoseStack itself is already
            // in true block-scale here, and only ModelPart's own translateAndRotate() divides its x/y/z by 16
            // before applying them (confirmed by decompiling the real method). A previous version of this line
            // passed TORSO_HEIGHT in raw, undivided, which placed the tail's root 12 blocks below the player
            // instead of 0.75 - invisible, not just misaligned. heightFraction/backOffset are small manual
            // fine-tune nudges (not pixel-unit anatomical constants), so they're intentionally left in block-scale.
            poseStack.translate(0F, TORSO_HEIGHT / 16F + heightFraction, backOffset);
        }
        else
        {
            poseStack.translate(0, entity.getBbHeight() * heightFraction, backOffset);
        }

        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        poseStack.popPose();
    }
}
