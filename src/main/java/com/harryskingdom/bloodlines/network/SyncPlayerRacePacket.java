package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: tells every client that can see a given player entity what race that player is, so wing render layers can
 * decide whether to draw on entities other than the local player. Curios used to give us this for free (it syncs
 * equipped item stacks to observers); now that flight/wings no longer go through Curios, we sync it ourselves.
 */
public class SyncPlayerRacePacket
{
    private final int entityId;
    private final String raceId;

    public SyncPlayerRacePacket(int entityId, Race race)
    {
        this(entityId, race == null ? "" : race.name());
    }

    private SyncPlayerRacePacket(int entityId, String raceId)
    {
        this.entityId = entityId;
        this.raceId = raceId;
    }

    public static void encode(SyncPlayerRacePacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.raceId);
    }

    public static SyncPlayerRacePacket decode(FriendlyByteBuf buf)
    {
        return new SyncPlayerRacePacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(SyncPlayerRacePacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientRaceCache.set(msg.entityId, msg.raceId)));
        ctx.setPacketHandled(true);
    }
}
