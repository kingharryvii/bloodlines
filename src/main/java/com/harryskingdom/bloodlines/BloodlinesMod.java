package com.harryskingdom.bloodlines;

import com.harryskingdom.bloodlines.integration.icarus.IcarusWingHooks;
import com.harryskingdom.bloodlines.item.BloodlinesItems;
import com.harryskingdom.bloodlines.loot.BloodlinesLootModifiers;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
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
        LOGGER.info("Bloodlines loaded successfully.");
    }
}
