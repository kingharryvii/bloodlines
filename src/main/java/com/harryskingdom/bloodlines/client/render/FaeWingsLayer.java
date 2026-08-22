package com.harryskingdom.bloodlines.client.render;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the Fae's articulated butterfly wings (see FaeWingModel) on Fae players. No Icarus item involved: Fae's
 * flight is fully native (see RaceFlightResource), so this is gated purely on race, and the animation state
 * machine below drives the four wing parts directly instead of relying on vanilla's elytraRotX/Y/Z fields.
 */
public class FaeWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");

    private static final float OPEN_SMOOTHING = 0.12F;
    private static final float FLAP_SPEED = 1.4F;

    // Target open amounts (0=folded, 1=fully spread) per state.
    private static final float OPEN_IDLE = 0.05F;
    private static final float OPEN_WALKING = 0.2F;
    private static final float OPEN_AIRBORNE = 1.0F;

    private static final Map<Integer, Float> OPEN_AMOUNT = new ConcurrentHashMap<>();

    private final FaeWingModel model;

    public FaeWingsLayer(RenderLayerParent<T, M> renderer, EntityModelSet modelSet)
    {
        super(renderer);
        this.model = new FaeWingModel(modelSet.bakeLayer(FaeWingModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (ClientRaceCache.get(entity.getId()) != Race.FAE || entity.isInvisible())
            return;

        boolean flying = entity instanceof Player player && player.getAbilities().flying;
        Vec3 movement = entity.getDeltaMovement();
        boolean airborne = flying || !entity.onGround();
        boolean walking = !airborne && (Math.abs(movement.x) + Math.abs(movement.z) > 0.02);

        float target = airborne ? OPEN_AIRBORNE : (walking ? OPEN_WALKING : OPEN_IDLE);
        float current = OPEN_AMOUNT.getOrDefault(entity.getId(), OPEN_IDLE);
        current += (target - current) * OPEN_SMOOTHING;
        OPEN_AMOUNT.put(entity.getId(), current);

        model.setPose(current, flying, ageInTicks * FLAP_SPEED);

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5, 0);
        poseStack.scale(1.3F, 1.3F, 1.3F);
        poseStack.translate(0, -entity.getBbHeight() * 0.5, 0);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    /** Drops the smoothed animation state for a player that's gone (logout, race change cleanup on rejoin, etc). */
    public static void clearState(int entityId)
    {
        OPEN_AMOUNT.remove(entityId);
    }
}
