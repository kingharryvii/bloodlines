package com.harryskingdom.bloodlines.item;

import com.harryskingdom.bloodlines.BloodlinesMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BloodlinesItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BloodlinesMod.MODID);

    public static final RegistryObject<Item> ORB_OF_ORIGIN = ITEMS.register("orb_of_origin",
            () -> new OrbOfOriginItem(new Item.Properties().stacksTo(1)));

    private BloodlinesItems() {}

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
