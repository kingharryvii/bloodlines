package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S - BloodlinesConfigScreen's Save button sends this, host and remote op alike (see the screen's own class doc
 * for why even the host goes over the network instead of writing locally). hasPermissions(2) is checked again
 * here even though the screen already hides Save from non-ops: the screen's gate only stops the normal client
 * from sending this packet, not a modified one crafted by hand, so the actual authorization boundary has to live
 * here, not in the GUI.
 * <p>
 * On success, broadcasts SyncBloodlinesConfigPacket to every connected player - without it, everyone's local
 * BloodlinesConfig.*.get() (including the editor's own, and every client's cooldown/duration HUD bars) would
 * keep reading whatever was cached at their own login, since Forge's SERVER-config sync never fires again after
 * that. That was the actual cause behind "I hit Save, but reopening the menu shows the old values" - the save
 * itself always worked, nothing was pushing the new numbers back out to any client afterward.
 */
public class UpdateBloodlinesConfigPacket
{
    private final int foodLevel;
    private final double exhaustion;
    private final double flySpeed;
    private final String faeTier;
    private final String angelkinTier;
    private final String demonkinTier;
    private final int cooldownSeconds;
    private final int durationSeconds;
    private final double orbRarityMultiplier;

    public UpdateBloodlinesConfigPacket(int foodLevel, double exhaustion, double flySpeed,
            BloodlinesConfig.MaxArmorTier faeTier, BloodlinesConfig.MaxArmorTier angelkinTier, BloodlinesConfig.MaxArmorTier demonkinTier,
            int cooldownSeconds, int durationSeconds, double orbRarityMultiplier)
    {
        this.foodLevel = foodLevel;
        this.exhaustion = exhaustion;
        this.flySpeed = flySpeed;
        this.faeTier = faeTier.name();
        this.angelkinTier = angelkinTier.name();
        this.demonkinTier = demonkinTier.name();
        this.cooldownSeconds = cooldownSeconds;
        this.durationSeconds = durationSeconds;
        this.orbRarityMultiplier = orbRarityMultiplier;
    }

    private UpdateBloodlinesConfigPacket(int foodLevel, double exhaustion, double flySpeed,
            String faeTier, String angelkinTier, String demonkinTier, int cooldownSeconds, int durationSeconds, double orbRarityMultiplier)
    {
        this.foodLevel = foodLevel;
        this.exhaustion = exhaustion;
        this.flySpeed = flySpeed;
        this.faeTier = faeTier;
        this.angelkinTier = angelkinTier;
        this.demonkinTier = demonkinTier;
        this.cooldownSeconds = cooldownSeconds;
        this.durationSeconds = durationSeconds;
        this.orbRarityMultiplier = orbRarityMultiplier;
    }

    public static void encode(UpdateBloodlinesConfigPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.foodLevel);
        buf.writeDouble(msg.exhaustion);
        buf.writeDouble(msg.flySpeed);
        buf.writeUtf(msg.faeTier);
        buf.writeUtf(msg.angelkinTier);
        buf.writeUtf(msg.demonkinTier);
        buf.writeVarInt(msg.cooldownSeconds);
        buf.writeVarInt(msg.durationSeconds);
        buf.writeDouble(msg.orbRarityMultiplier);
    }

    public static UpdateBloodlinesConfigPacket decode(FriendlyByteBuf buf)
    {
        return new UpdateBloodlinesConfigPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readDouble());
    }

    public static void handle(UpdateBloodlinesConfigPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = ctx.getSender();
            if (player == null)
                return;

            if (!player.hasPermissions(2))
            {
                player.sendSystemMessage(Component.literal("You don't have permission to change the Bloodlines config.").withStyle(ChatFormatting.RED));
                return;
            }

            BloodlinesConfig.MaxArmorTier faeTier = BloodlinesConfig.MaxArmorTier.tryParse(msg.faeTier).orElse(BloodlinesConfig.FAE_MAX_ARMOR_TIER.get());
            BloodlinesConfig.MaxArmorTier angelkinTier = BloodlinesConfig.MaxArmorTier.tryParse(msg.angelkinTier).orElse(BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.get());
            BloodlinesConfig.MaxArmorTier demonkinTier = BloodlinesConfig.MaxArmorTier.tryParse(msg.demonkinTier).orElse(BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.get());

            BloodlinesConfig.FAE_REQUIRED_FOOD_LEVEL.set(Mth.clamp(msg.foodLevel, BloodlinesConfig.FAE_FOOD_LEVEL_MIN, BloodlinesConfig.FAE_FOOD_LEVEL_MAX));
            BloodlinesConfig.FAE_EXHAUSTION_PER_BOOST_TICK.set(Mth.clamp(msg.exhaustion, BloodlinesConfig.FAE_EXHAUSTION_MIN, BloodlinesConfig.FAE_EXHAUSTION_MAX));
            BloodlinesConfig.FAE_FLYING_SPEED.set(Mth.clamp(msg.flySpeed, BloodlinesConfig.FAE_FLY_SPEED_MIN, BloodlinesConfig.FAE_FLY_SPEED_MAX));
            BloodlinesConfig.FAE_MAX_ARMOR_TIER.set(faeTier);
            BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.set(angelkinTier);
            BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.set(demonkinTier);
            BloodlinesConfig.ABILITY_COOLDOWN_SECONDS.set(Mth.clamp(msg.cooldownSeconds, BloodlinesConfig.ABILITY_SECONDS_MIN, BloodlinesConfig.ABILITY_COOLDOWN_MAX));
            BloodlinesConfig.ABILITY_DURATION_SECONDS.set(Mth.clamp(msg.durationSeconds, BloodlinesConfig.ABILITY_SECONDS_MIN, BloodlinesConfig.ABILITY_DURATION_MAX));
            BloodlinesConfig.ORB_SPAWN_RARITY_MULTIPLIER.set(Mth.clamp(msg.orbRarityMultiplier, BloodlinesConfig.ORB_RARITY_MULTIPLIER_MIN, BloodlinesConfig.ORB_RARITY_MULTIPLIER_MAX));
            BloodlinesConfig.SPEC.save();

            BloodlinesNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncBloodlinesConfigPacket());
            player.sendSystemMessage(Component.literal("Bloodlines config updated.").withStyle(ChatFormatting.GREEN));
        });
        ctx.setPacketHandled(true);
    }
}
