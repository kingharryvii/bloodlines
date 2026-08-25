package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.UseRaceAbilityPacket;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fae's wing pose/animation is driven entirely by FaeWingsLayer per-frame; this just forwards ability key input. */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BloodlinesClientEvents
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        while (BloodlinesKeyMappings.USE_PRIMARY_ABILITY.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new UseRaceAbilityPacket(false));

        while (BloodlinesKeyMappings.USE_SECONDARY_ABILITY.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new UseRaceAbilityPacket(true));
    }

    /**
     * Hides Merfolk's legs while their fish tail is showing (TailLayer handles showing the tail itself). This
     * has to happen here, in a Pre event, rather than inside TailLayer - RenderLayers only run AFTER the base
     * PlayerModel has already drawn for the frame, so a mutation made there always arrives one frame late and,
     * since something re-marks the parts visible before the next base draw, never actually took hold. Pre fires
     * before that base draw, so setting .visible here actually reaches it - same timing real MerMod gets via a
     * Mixin into PlayerModel, achieved here through Forge's own event instead of bytecode weaving.
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event)
    {
        Player player = event.getEntity();
        boolean showTail = ClientRaceCache.get(player.getId()) == Race.MERFOLK && player.isInWater();

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.rightLeg.visible = !showTail;
        model.leftLeg.visible = !showTail;
        model.rightPants.visible = !showTail;
        model.leftPants.visible = !showTail;
    }
}
