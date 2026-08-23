package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.client.race.SeraphFlapTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: tells every client tracking this player that they just flapped, so remote-player wings animate too. */
public class SyncSeraphFlapPacket
{
    private final int entityId;

    public SyncSeraphFlapPacket(int entityId)
    {
        this.entityId = entityId;
    }

    public static void encode(SyncSeraphFlapPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.entityId);
    }

    public static SyncSeraphFlapPacket decode(FriendlyByteBuf buf)
    {
        return new SyncSeraphFlapPacket(buf.readVarInt());
    }

    public static void handle(SyncSeraphFlapPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SeraphFlapTracker.recordFlap(msg.entityId)));
        ctx.setPacketHandled(true);
    }
}
