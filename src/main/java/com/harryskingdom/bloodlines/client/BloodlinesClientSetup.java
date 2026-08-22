package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.render.FaeWingsLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BloodlinesClientSetup
{
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(BloodlinesKeyMappings.USE_PRIMARY_ABILITY);
        event.register(BloodlinesKeyMappings.USE_SECONDARY_ABILITY);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        for (String skin : event.getSkins())
        {
            PlayerRenderer renderer = event.getSkin(skin);
            renderer.addLayer(new FaeWingsLayer<>(renderer, event.getEntityModels()));
        }
    }
}
