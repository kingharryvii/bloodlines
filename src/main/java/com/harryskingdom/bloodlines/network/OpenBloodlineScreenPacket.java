package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.client.BloodlinesClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenBloodlineScreenPacket
{
    public static void encode(OpenBloodlineScreenPacket msg, FriendlyByteBuf buf) {}

    public static OpenBloodlineScreenPacket decode(FriendlyByteBuf buf)
    {
        return new OpenBloodlineScreenPacket();
    }

    public static void handle(OpenBloodlineScreenPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BloodlinesClient::openBloodlineScreen));
        ctx.setPacketHandled(true);
    }
}
