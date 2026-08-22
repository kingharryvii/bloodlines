package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.UseRaceAbilityPacket;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fae's butterfly-elytra wing pose is driven by the same public elytraRotX/Y/Z fields vanilla uses to animate a
 * real elytra, so we can pose them here each tick without touching Icarus or vanilla's own model code at all.
 * Logic ported from Medieval Origins Revival's own client-side flap animation (CC BY 4.0, credit muon-rw).
 */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BloodlinesClientEvents
{
    private static final double GROUND_DISTANCE_THRESHOLD = 0.1;
    private static final float WINGS_SPREAD_X = 1.4981317F;
    private static final float WINGS_SPREAD_Y = 0.58726646F;
    private static final float WINGS_SPREAD_Z = -0.5F - (float) Math.PI / 4F;

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

        for (AbstractClientPlayer player : minecraft.level.players())
        {
            if (ClientRaceCache.get(player.getId()) == Race.FAE)
                updateFaeWingFlap(player);
        }
    }

    private static void updateFaeWingFlap(AbstractClientPlayer player)
    {
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
