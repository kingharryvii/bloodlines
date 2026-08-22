package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.race.RaceHoverState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: reports whether the local player is currently holding jump, sent while on a flying race and only on change. */
public class HoverInputPacket
{
    private final boolean jumping;

    public HoverInputPacket(boolean jumping)
    {
        this.jumping = jumping;
    }

    public static void encode(HoverInputPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.jumping);
    }

    public static HoverInputPacket decode(FriendlyByteBuf buf)
    {
        return new HoverInputPacket(buf.readBoolean());
    }

    public static void handle(HoverInputPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = ctx.getSender();
            if (player != null)
                RaceHoverState.setJumping(player, msg.jumping);
        });
        ctx.setPacketHandled(true);
    }
}
