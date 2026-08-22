package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.UseRaceAbilityPacket;
import net.minecraftforge.api.distmarker.Dist;
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
}
