package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.RaceSelection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Locale;
import java.util.function.Supplier;

public class SelectRacePacket
{
    private final String raceId;

    public SelectRacePacket(Race race)
    {
        this.raceId = race.name();
    }

    private SelectRacePacket(String raceId)
    {
        this.raceId = raceId;
    }

    public static void encode(SelectRacePacket msg, FriendlyByteBuf buf)
    {
        buf.writeUtf(msg.raceId);
    }

    public static SelectRacePacket decode(FriendlyByteBuf buf)
    {
        return new SelectRacePacket(buf.readUtf());
    }

    public static void handle(SelectRacePacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;

            Race race;
            try
            {
                race = Race.valueOf(msg.raceId.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException e)
            {
                return;
            }

            switch (RaceSelection.select(player, race))
            {
                case SUCCESS ->
                {
                    player.sendSystemMessage(Component.literal("You have chosen the bloodline of ")
                            .append(Component.literal(race.getDisplayName()).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(".")));
                    BloodlinesNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                            new SyncPlayerRacePacket(player.getId(), race));
                }
                case ALREADY_CHOSEN -> player.sendSystemMessage(Component.literal("You have already chosen your bloodline.").withStyle(ChatFormatting.RED));
                case LOCKED -> player.sendSystemMessage(Component.literal("That bloodline has not been unlocked yet.").withStyle(ChatFormatting.RED));
            }
        });
        ctx.setPacketHandled(true);
    }
}
