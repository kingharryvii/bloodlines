package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.render.RaceWingModel;
import com.harryskingdom.bloodlines.client.render.RacialWingsLayer;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import dev.cammiescorner.icarus.api.client.IcarusAPIClient;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BloodlinesClientSetup
{
    // Fae only - Seraph uses Icarus's own native wing render/animation instead (see IcarusIntegration).
    private static final ResourceLocation FAE_WINGS_TEXTURE = new ResourceLocation(BloodlinesMod.MODID, "textures/entity/fae_wings.png");

    // Wing panel width is a fixed 16 units (1 block); height derived from the texture's real aspect ratio
    // (256x180px) so the wing shape isn't stretched.
    private static final ModelLayerLocation FAE_WING_LAYER = new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "fae_wings"), "main");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(BloodlinesKeyMappings.USE_PRIMARY_ABILITY);
        event.register(BloodlinesKeyMappings.USE_SECONDARY_ABILITY);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(FAE_WING_LAYER, () -> RaceWingModel.createBodyLayer(16F, 11.25F));
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        for (String skin : event.getSkins())
        {
            PlayerRenderer renderer = event.getSkin(skin);
            renderer.addLayer(new RacialWingsLayer<>(renderer, event.getEntityModels().bakeLayer(FAE_WING_LAYER), Race.FAE, FAE_WINGS_TEXTURE));
        }
    }

    /**
     * Fae still wear a real Icarus wing item (IcarusIntegration) so Icarus's own flight mechanics work, but
     * Icarus's own wing model would otherwise render right alongside our custom one. addRenderPredicate is
     * Icarus's own sanctioned extension point for suppressing its wing render conditionally - much more reliable
     * than the two things tried before (Curios' render toggle, a transparent texture override), neither of which
     * actually stopped Icarus from drawing its wing.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        if (!ModList.get().isLoaded("icarus"))
            return;

        event.enqueueWork(() -> IcarusAPIClient.addRenderPredicate(entity -> ClientRaceCache.get(entity.getId()) != Race.FAE));
    }
}
