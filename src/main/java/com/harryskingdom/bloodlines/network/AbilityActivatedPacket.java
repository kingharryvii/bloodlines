package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.client.race.AbilityHudState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C, no payload - tells the activating player's own client "your ability just fired", so the cooldown/duration
 * HUD bars (see AbilityHudState/AbilityCooldownOverlay) have a clock to count from. The actual cooldown and
 * duration lengths aren't sent since RaceAbility.cooldownTicks()/durationTicks() read from BloodlinesConfig, a
 * SERVER-type config Forge auto-syncs to every connecting client - both sides already agree on the same numbers
 * by the time this fires, so this packet is purely a "start the clock now" trigger.
 */
public class AbilityActivatedPacket
{
    public static void encode(AbilityActivatedPacket msg, FriendlyByteBuf buf) {}

    public static AbilityActivatedPacket decode(FriendlyByteBuf buf)
    {
        return new AbilityActivatedPacket();
    }

    public static void handle(AbilityActivatedPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AbilityHudState::onActivated));
        ctx.setPacketHandled(true);
    }
}
