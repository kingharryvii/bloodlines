package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.race.PlayerRaceCapability;
import com.harryskingdom.bloodlines.race.RaceAbility;
import com.harryskingdom.bloodlines.race.RaceAbilityCooldowns;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseRaceAbilityPacket
{
    private final boolean secondary;

    public UseRaceAbilityPacket(boolean secondary)
    {
        this.secondary = secondary;
    }

    public static void encode(UseRaceAbilityPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.secondary);
    }

    public static UseRaceAbilityPacket decode(FriendlyByteBuf buf)
    {
        return new UseRaceAbilityPacket(buf.readBoolean());
    }

    public static void handle(UseRaceAbilityPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;

            PlayerRaceCapability.get(player).ifPresent(data -> data.getRace().ifPresent(race ->
            {
                if (msg.secondary)
                {
                    handleSecondary(player, race);
                    return;
                }

                String abilityName = RaceAbility.nameFor(race);
                if (abilityName == null)
                {
                    player.sendSystemMessage(Component.literal("This bloodline has no primary ability yet.").withStyle(ChatFormatting.GRAY));
                    return;
                }

                if (!RaceAbilityCooldowns.isReady(player, RaceAbility.COOLDOWN_TICKS))
                {
                    int seconds = RaceAbilityCooldowns.ticksRemaining(player, RaceAbility.COOLDOWN_TICKS) / 20;
                    player.sendSystemMessage(Component.literal(abilityName + " is on cooldown (" + seconds + "s).").withStyle(ChatFormatting.RED));
                    return;
                }

                if (RaceAbility.activate(player, race))
                {
                    RaceAbilityCooldowns.markUsed(player);
                    player.sendSystemMessage(Component.literal(abilityName + "!").withStyle(ChatFormatting.AQUA));
                }
            }));
        });
        ctx.setPacketHandled(true);
    }

    private static void handleSecondary(ServerPlayer player, com.harryskingdom.bloodlines.race.Race race)
    {
        String secondaryName = RaceAbility.secondaryNameFor(race);
        if (secondaryName == null)
        {
            player.sendSystemMessage(Component.literal("This bloodline has no secondary ability.").withStyle(ChatFormatting.GRAY));
            return;
        }

        // Hover is the only secondary ability so far; free toggle, no cooldown.
        RaceAbility.toggleHover(player);
        String state = RaceAbility.isHovering(player) ? "engaged" : "disengaged";
        player.sendSystemMessage(Component.literal(secondaryName + " " + state + ".").withStyle(ChatFormatting.AQUA));
    }
}
