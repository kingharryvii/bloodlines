package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.HoverInputPacket;
import com.harryskingdom.bloodlines.network.UseRaceAbilityPacket;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.RaceAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fae's custom wing pose is driven by the same public elytraRotX/Y/Z fields vanilla uses to animate a real
 * elytra, so we can pose them here each tick without touching vanilla's own model code at all - this keeps
 * working unchanged under real Icarus flight since isFallFlying()/movement are genuine vanilla state regardless
 * of what triggered them. Seraph uses Icarus's own native wings/animation instead, so isn't handled here. Flap
 * logic ported from Medieval Origins Revival's own client-side animation (CC BY 4.0, credit muon-rw).
 */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BloodlinesClientEvents
{
    private static final double GROUND_DISTANCE_THRESHOLD = 0.1;
    private static final float WINGS_SPREAD_X = 1.4981317F;
    private static final float WINGS_SPREAD_Y = 0.58726646F;
    private static final float WINGS_SPREAD_Z = -0.5F - (float) Math.PI / 4F;

    private static boolean lastSentJumping = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        while (BloodlinesKeyMappings.USE_PRIMARY_ABILITY.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new UseRaceAbilityPacket(false));

        while (BloodlinesKeyMappings.USE_SECONDARY_ABILITY.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new UseRaceAbilityPacket(true));

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        // Hover's vertical control needs the jump-held state synced to the server each time it changes -
        // hasEffect(LEVITATION) alone is a reliable gate on its own. Double-jump flight is handled entirely by
        // the real Icarus mod now (IcarusIntegration equips the wing item, Icarus does the rest natively) - no
        // packet or per-tick handling of our own needed for it at all.
        if (minecraft.player != null && minecraft.player.hasEffect(MobEffects.LEVITATION))
        {
            boolean jumping = minecraft.options.keyJump.isDown();
            RaceAbility.updateHoverMovement(minecraft.player, jumping);
            RaceAbility.updateHoverWingState(minecraft.player, ClientRaceCache.get(minecraft.player.getId()));

            if (jumping != lastSentJumping)
            {
                BloodlinesNetwork.CHANNEL.sendToServer(new HoverInputPacket(jumping));
                lastSentJumping = jumping;
            }
        }

        for (AbstractClientPlayer player : minecraft.level.players())
        {
            if (ClientRaceCache.get(player.getId()) == Race.FAE)
                updateWingFlap(player);
        }
    }

    private static void updateWingFlap(AbstractClientPlayer player)
    {
        // Hover holds a fixed vertical position rather than building real forward speed, so the movement-based
        // flap strength below (which reads off actual velocity) would barely move - use a steady time-based
        // flap instead, so wings actively beat to hold position rather than freezing in a static spread.
        if (player.hasEffect(MobEffects.LEVITATION))
        {
            float flapStrength = 0.5F + 0.5F * (float) Math.sin(player.tickCount * 0.4F);
            player.elytraRotX = WINGS_SPREAD_X * flapStrength;
            player.elytraRotY = WINGS_SPREAD_Y * flapStrength;
            player.elytraRotZ = WINGS_SPREAD_Z * flapStrength;
            return;
        }

        if (player.getAbilities().flying)
        {
            player.elytraRotX = WINGS_SPREAD_X;
            player.elytraRotY = WINGS_SPREAD_Y;
            player.elytraRotZ = WINGS_SPREAD_Z;
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.y < -0.5)
            return;

        double normalizedY = movement.y * 2.5;
        double speedMagnitude = Math.sqrt(movement.x * movement.x + movement.z * movement.z + Math.max(normalizedY, 0) * Math.max(normalizedY, 0)) * 4;
        float flapStrength = (float) Math.min(speedMagnitude, 1.0);

        Vec3 start = player.position();
        Vec3 end = start.add(0, -1, 0);
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean closeToGround = hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(start) <= GROUND_DISTANCE_THRESHOLD;

        if (player.isFallFlying() || normalizedY > 0.1 || (!closeToGround && (Math.abs(movement.x) + Math.abs(movement.z) > 0.1)))
        {
            player.elytraRotX = WINGS_SPREAD_X * flapStrength;
            player.elytraRotY = WINGS_SPREAD_Y * flapStrength;
            player.elytraRotZ = WINGS_SPREAD_Z * flapStrength;
        }
    }
}
