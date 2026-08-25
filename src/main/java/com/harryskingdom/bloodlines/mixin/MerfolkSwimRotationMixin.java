package com.harryskingdom.bloodlines.mixin;

import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Merfolk-only fix for the tail detaching during steep dives (sprint-swimming with sneak held). Traced from the
 * real MerMod mod's own AvatarRendererMixin#onSetupRotations, which solves the identical "tail fights the body's
 * own swim rotation" problem by fully replacing vanilla's swim-tilt rather than trying to correct it afterward
 * from a RenderLayer - the same injection point (right after the super.setupRotations() call inside the
 * swimAmount &gt; 0 branch), adapted to 1.20.1's PlayerRenderer (which doesn't have MerMod's newer render-state
 * split, so this reads straight off the entity instead of a captured state object).
 * <p>
 * Decompiled and verified directly (Vineflower against the real mapped jar, not guessed) - vanilla's own real
 * chain here is:
 * <pre>
 *   float f4 = Mth.lerp(swimAmount, 0, -90 - entity.getXRot());
 *   poseStack.mulPose(Axis.XP.rotationDegrees(f4));
 *   if (isVisuallySwimming) poseStack.translate(0, -1, 0.3F);
 * </pre>
 * A pure rotation always leaves the coordinate origin exactly where it was - that's a basic property of
 * rotation matrices, R applied to the zero vector is still zero - so on its own this rotation was never the
 * source of drift. Two different attempts to "fix" it both broke that property instead of relying on it:
 * <p>
 * The first (TailLayer cancelling and reapplying this exact chain around its own translate) was correct for a
 * level swim, but the small extra translate(0,-1,0.3) - the ONLY piece of vanilla's own formula that isn't a
 * pure rotation - doesn't fully cancel out of that construction, leaving a small angle-dependent residual that
 * only became visible at the large XRot values a steep dive produces.
 * <p>
 * The second (this Mixin's own first version, translate-to-hip / rotate / translate-back, the textbook "rotate
 * around an arbitrary pivot" identity) is correct for transforming a single point, but wrongly applied as a
 * PoseStack prefix: it moves where the origin itself lands - by {@code hip - R(hip)}, which grows with the
 * rotation angle - so anything translated afterward (TailLayer's own anchor, unchanged) was starting from a
 * moving reference point instead of a fixed one. That's what reintroduced "the tail base migrates toward the
 * shoulders while swimming," this time from the Mixin itself rather than from TailLayer.
 * <p>
 * The fix that's actually structurally correct: apply the plain rotation, exactly like vanilla, with no pivot
 * trick - and simply drop vanilla's own extra translate(0,-1,0.3), since replacing vanilla's rotation instead of
 * layering on top of it means there's no obligation to keep that small recentering nudge too. With no non-
 * rotation component left in the chain at all, the origin is mathematically guaranteed to stay exactly where it
 * started, at every XRot, for the entire swimAmount range - not just approximately fixed, provably fixed. The
 * cost is that the base body's own sprint-swim pose loses that small nudge, only while playing Merfolk - not
 * perceptible next to a tail that no longer drifts.
 * <p>
 * Isolated on purpose: gated on {@link ClientRaceCache} so every other race's swim rendering, and everything
 * else in this mod, is completely untouched - removing this class and its two registration lines (mods.toml's
 * {@code [[mixins]]} entry and the annotationProcessor line in build.gradle) fully reverts it with nothing else
 * to unwind.
 */
@Mixin(PlayerRenderer.class)
public abstract class MerfolkSwimRotationMixin
{
    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void bloodlines$fixMerfolkSwimTilt(AbstractClientPlayer player, PoseStack poseStack, float bob, float yBodyRot, float partialTicks, CallbackInfo ci)
    {
        if (ClientRaceCache.get(player.getId()) != Race.MERFOLK)
            return;

        // TailLayer's swapLegsInWater already guarantees the tail (and therefore the need for this fix) only
        // ever shows while entity.isInWater() is true, so this always takes vanilla's "in water" branch.
        float swimAmount = player.getSwimAmount(partialTicks);
        float targetAngle = -90F - player.getXRot();
        float angle = Mth.lerp(swimAmount, 0F, targetAngle);

        poseStack.mulPose(Axis.XP.rotationDegrees(angle));

        ci.cancel();
    }
}
