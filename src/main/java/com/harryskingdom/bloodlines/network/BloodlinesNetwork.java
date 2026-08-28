package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.BloodlinesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BloodlinesNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BloodlinesMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private BloodlinesNetwork() {}

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenBloodlineScreenPacket.class,
                OpenBloodlineScreenPacket::encode, OpenBloodlineScreenPacket::decode, OpenBloodlineScreenPacket::handle);
        CHANNEL.registerMessage(id++, SelectRacePacket.class,
                SelectRacePacket::encode, SelectRacePacket::decode, SelectRacePacket::handle);
        CHANNEL.registerMessage(id++, UseRaceAbilityPacket.class,
                UseRaceAbilityPacket::encode, UseRaceAbilityPacket::decode, UseRaceAbilityPacket::handle);
        CHANNEL.registerMessage(id++, SyncPlayerRacePacket.class,
                SyncPlayerRacePacket::encode, SyncPlayerRacePacket::decode, SyncPlayerRacePacket::handle);
        CHANNEL.registerMessage(id++, AbilityActivatedPacket.class,
                AbilityActivatedPacket::encode, AbilityActivatedPacket::decode, AbilityActivatedPacket::handle);
        CHANNEL.registerMessage(id++, UpdateBloodlinesConfigPacket.class,
                UpdateBloodlinesConfigPacket::encode, UpdateBloodlinesConfigPacket::decode, UpdateBloodlinesConfigPacket::handle);
    }
}
