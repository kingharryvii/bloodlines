package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.client.race.AbilityHudState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C - tells the activating player's own client "your primary/secondary ability just fired", so that slot's own
 * cooldown/duration HUD bar (see AbilityHudState/AbilityCooldownOverlay - primary and secondary each keep an
 * independent clock, same as their cooldowns are tracked independently server-side in RaceAbilityCooldowns) has
 * a clock to count from. The actual cooldown and duration lengths aren't sent since RaceAbility's
 * cooldownTicks()/secondaryCooldownTicks()/durationTicks()/secondaryDurationTicks() read from BloodlinesConfig, a
 * SERVER-type config Forge auto-syncs to every connecting client - both sides already agree on the same numbers
 * by the time this fires, so this packet is purely a "start this slot's clock now" trigger.
 */
public class AbilityActivatedPacket
{
    private final boolean secondary;

    public AbilityActivatedPacket(boolean secondary)
    {
        this.secondary = secondary;
    }

    public static void encode(AbilityActivatedPacket msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.secondary);
    }

    public static AbilityActivatedPacket decode(FriendlyByteBuf buf)
    {
        return new AbilityActivatedPacket(buf.readBoolean());
    }

    public static void handle(AbilityActivatedPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AbilityHudState.onActivated(msg.secondary)));
        ctx.setPacketHandled(true);
    }
}
