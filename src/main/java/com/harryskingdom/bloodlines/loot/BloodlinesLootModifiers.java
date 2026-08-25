package com.harryskingdom.bloodlines.loot;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BloodlinesLootModifiers
{
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BloodlinesMod.MODID);

    public static final RegistryObject<Codec<OrbOfOriginLootModifier>> ORB_OF_ORIGIN =
            MODIFIERS.register("orb_of_origin", () -> OrbOfOriginLootModifier.CODEC);

    private BloodlinesLootModifiers() {}

    public static void register(IEventBus modEventBus)
    {
        MODIFIERS.register(modEventBus);
    }
}
