package com.harryskingdom.bloodlines;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import com.harryskingdom.bloodlines.integration.icarus.IcarusWingHooks;
import com.harryskingdom.bloodlines.item.BloodlinesItems;
import com.harryskingdom.bloodlines.loot.BloodlinesLootModifiers;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BloodlinesMod.MODID)
public class BloodlinesMod
{
    public static final String MODID = "harrys_bloodlines";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BloodlinesMod(FMLJavaModLoadingContext context)
    {
        BloodlinesNetwork.register();
        BloodlinesItems.register(context.getModEventBus());
        BloodlinesLootModifiers.register(context.getModEventBus());
        // Runs after Icarus's own FMLCommonSetupEvent handler sets its real hasWings/getEquippedWings values -
        // mods.toml declares ordering="AFTER" on the icarus dependency specifically so this is guaranteed, not
        // just likely (see IcarusWingHooks for why the order matters).
        context.getModEventBus().addListener((FMLCommonSetupEvent event) -> IcarusWingHooks.install());

        // SERVER type (not COMMON) - see BloodlinesConfig's own header comment for why: these values need to be
        // the same on every connecting client, not just locally consistent, and Forge only auto-syncs SERVER
        // configs.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BloodlinesConfig.SPEC);
        // The config screen class touches client-only widgets (EditBox, Screen, ...) - isolated behind
        // DistExecutor so a dedicated server, which never loads client classes, never attempts to either.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.harryskingdom.bloodlines.client.BloodlinesConfigScreen::register);

        LOGGER.info("Bloodlines loaded successfully.");
    }
}
