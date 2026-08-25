package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.client.model.HeadAccessoryModel;
import com.harryskingdom.bloodlines.client.model.TailModel;
import com.harryskingdom.bloodlines.client.render.FaeWingsLayer;
import com.harryskingdom.bloodlines.client.render.HeadAccessoryLayer;
import com.harryskingdom.bloodlines.client.render.TailLayer;
import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BloodlinesClientSetup
{
    private static final ModelLayerLocation BEASTKIN_TAIL =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "beastkin_tail"), "main");
    private static final ModelLayerLocation MERFOLK_TAIL =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "merfolk_tail"), "main");
    private static final ModelLayerLocation HALO =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "halo"), "main");
    private static final ModelLayerLocation HORNS =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "horns"), "main");
    private static final ModelLayerLocation ELF_EARS =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "elf_ears"), "main");
    private static final ModelLayerLocation CAT_EARS =
            new ModelLayerLocation(new ResourceLocation(BloodlinesMod.MODID, "cat_ears"), "main");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(BloodlinesKeyMappings.USE_PRIMARY_ABILITY);
        event.register(BloodlinesKeyMappings.USE_SECONDARY_ABILITY);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAboveAll("ability_cooldown", new AbilityCooldownOverlay());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(BEASTKIN_TAIL, TailModel::createBeastkinLayer);
        event.registerLayerDefinition(MERFOLK_TAIL, TailModel::createMerfolkLayer);
        event.registerLayerDefinition(HALO, HeadAccessoryModel::createHaloLayer);
        event.registerLayerDefinition(HORNS, HeadAccessoryModel::createHornsLayer);
        event.registerLayerDefinition(ELF_EARS, HeadAccessoryModel::createElfEarsLayer);
        event.registerLayerDefinition(CAT_EARS, HeadAccessoryModel::createCatEarsLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        for (String skin : event.getSkins())
        {
            PlayerRenderer renderer = event.getSkin(skin);
            renderer.addLayer(new FaeWingsLayer<>(renderer, event.getEntityModels()));
            renderer.addLayer(new TailLayer<>(renderer, event.getEntityModels(), BEASTKIN_TAIL, "beastkin_tail",
                    Race.BEASTKIN, new float[] {0.75F, 0.6F, 0.5F}, 0.2F, false, 0.32F, 0.16F, false, false));
            renderer.addLayer(new TailLayer<>(renderer, event.getEntityModels(), MERFOLK_TAIL, "merfolk_tail",
                    Race.MERFOLK, new float[] {0.02F, 0.03F, 0.03F, 0.04F, 0.04F, 0.03F, 0.02F}, 0F, true, 0.28F, 0.02F, true, false));
            renderer.addLayer(new HeadAccessoryLayer<>(renderer, event.getEntityModels(), HALO, "halo", true, Race.SERAPH));
            renderer.addLayer(new HeadAccessoryLayer<>(renderer, event.getEntityModels(), HORNS, "horns", false, Race.DEMON));
            renderer.addLayer(new HeadAccessoryLayer<>(renderer, event.getEntityModels(), ELF_EARS, "elf_ears", false,
                    Race.WOOD_ELF, Race.HIGH_ELF, Race.MOON_ELF));
            renderer.addLayer(new HeadAccessoryLayer<>(renderer, event.getEntityModels(), CAT_EARS, "cat_ears", false, Race.BEASTKIN));
        }
    }
}
