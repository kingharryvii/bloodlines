package com.harryskingdom.bloodlines.mixin;

import com.harryskingdom.bloodlines.integration.icarus.IcarusIntegration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import dev.cammiescorner.icarus.client.renderers.WingsLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Icarus draws its own wing model directly, with no way to hide it via Curios' cosmetic-render toggle (confirmed
 * by testing) or Icarus's own addRenderPredicate client API (also confirmed by testing - it doesn't suppress this
 * layer). Fae still needs a real Icarus wing item equipped for the flight mechanic to work, so instead we cancel
 * Icarus's own render call for Fae players specifically, leaving FaeWingsLayer as the only thing drawn.
 * WingsLayer has both a generic-typed "render" method and a compiler-generated bridge method (erased to Entity)
 * matching vanilla's abstract RenderLayer#render - hooking both since it's not confirmed which one (or both)
 * actually contributes to the drawn output. The bridge injection is require = 0 (non-fatal if its target isn't
 * found) so a failure there can't silently take down the whole mixin class, including the direct injection.
 */
@Mixin(value = WingsLayer.class, remap = false)
public abstract class IcarusWingsLayerMixin
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean loggedBridge = false;
    private static boolean loggedDirect = false;

    @Inject(
            method = "m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void bloodlines$skipBridge(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Entity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
    {
        if (!loggedBridge)
        {
            LOGGER.info("[Bloodlines] WingsLayer bridge render() is being called (entity={})", entity);
            loggedBridge = true;
        }

        if (entity instanceof LivingEntity living && IcarusIntegration.isFaeWingsEquipped(living))
        {
            LOGGER.info("[Bloodlines] cancelling Icarus wing render (bridge) for Fae entity {}", entity);
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bloodlines$skipDirect(PoseStack poseStack, MultiBufferSource buffer, int packedLight, LivingEntity entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
    {
        if (!loggedDirect)
        {
            LOGGER.info("[Bloodlines] WingsLayer direct render() is being called (entity={})", entity);
            loggedDirect = true;
        }

        if (IcarusIntegration.isFaeWingsEquipped(entity))
        {
            LOGGER.info("[Bloodlines] cancelling Icarus wing render (direct) for Fae entity {}", entity);
            ci.cancel();
        }
    }
}
