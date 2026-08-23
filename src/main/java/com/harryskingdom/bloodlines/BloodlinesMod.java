package com.harryskingdom.bloodlines;

import com.harryskingdom.bloodlines.item.BloodlinesItems;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
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
        LOGGER.info("Harry's Kingdom: Bloodlines loaded successfully.");
    }
}
