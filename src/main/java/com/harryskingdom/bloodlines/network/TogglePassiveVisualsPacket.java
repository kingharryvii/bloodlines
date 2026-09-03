package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.race.PlayerRaceCapability;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.RaceEffects;
import com.harryskingdom.bloodlines.race.RaceStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S, no payload - the client just asks to flip its own passive-visuals toggle (Night Vision, Slow Falling).
 * Server is the sole authority on the actual new state, same reasoning as everywhere else in this mod: it flips
 * its own stored value and reapplies race effects immediately so the change is felt right away, rather than
 * waiting for the next natural reapply. The keybind itself stays a neutral "Toggle Racial Effects" label (one
 * key covers every race), but the chat feedback names whichever effect(s) the player's own race actually has -
 * "Slow Fall" for Fae, "Night Vision" for the always-on races and for Merfolk's underwater-only grant - rather
 * than a generic message that wouldn't mean anything concrete to whoever just pressed the key.
 */
public class TogglePassiveVisualsPacket
{
    public static void encode(TogglePassiveVisualsPacket msg, FriendlyByteBuf buf) {}

    public static TogglePassiveVisualsPacket decode(FriendlyByteBuf buf)
    {
        return new TogglePassiveVisualsPacket();
    }

    public static void handle(TogglePassiveVisualsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;

            PlayerRaceCapability.get(player).ifPresent(data ->
            {
                boolean enabled = !data.isPassiveVisualsEnabled();
                data.setPassiveVisualsEnabled(enabled);

                data.getRace().ifPresent(race ->
                {
                    RaceEffects.apply(player, race);

                    player.sendSystemMessage(Component.literal(effectLabel(race) + ": " + (enabled ? "ON" : "OFF"))
                            .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY));
                });
            });
        });
        ctx.setPacketHandled(true);
    }

    /** Names whichever of the toggle's two effects this specific race actually grants, for the chat feedback. */
    private static String effectLabel(Race race)
    {
        RaceStats stats = RaceStats.of(race);
        List<String> affected = new ArrayList<>();

        if (stats.slowFalling())
            affected.add("Slow Fall");
        if (stats.nightVision() || race == Race.MERFOLK)
            affected.add("Night Vision");

        return affected.isEmpty() ? "Passive effects" : String.join(" & ", affected);
    }
}
