package com.harryskingdom.bloodlines.network;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C, broadcast to every connected player right after UpdateBloodlinesConfigPacket's handler successfully
 * applies a change. Forge's own SERVER-config sync only ever fires once, during a client's login handshake -
 * there's nothing built in that pushes an updated value out to clients already connected when a live change
 * happens. Without this, every client's BloodlinesConfig.*.get() (BloodlinesConfigScreen's own displayed rows
 * included) would keep reading whatever was cached at login, making a real, successful save look like it did
 * nothing the moment you reopened the screen to check it.
 * <p>
 * Applies with .set() only, never .save() - a client has nothing to persist to its own disk, and calling
 * ModConfig.save() here would hit the exact ClassCastException UpdateBloodlinesConfigPacket's own class doc
 * describes for a synced (non-host) client's config.
 */
public class SyncBloodlinesConfigPacket
{
    private final int foodLevel;
    private final double exhaustion;
    private final double flySpeed;
    private final String faeTier;
    private final String angelkinTier;
    private final String demonkinTier;
    private final int cooldownSeconds;
    private final int durationSeconds;
    private final int secondaryCooldownSeconds;
    private final int secondaryDurationSeconds;
    private final double orbRarityMultiplier;

    public SyncBloodlinesConfigPacket()
    {
        this.foodLevel = BloodlinesConfig.FAE_REQUIRED_FOOD_LEVEL.get();
        this.exhaustion = BloodlinesConfig.FAE_EXHAUSTION_PER_BOOST_TICK.get();
        this.flySpeed = BloodlinesConfig.FAE_FLYING_SPEED.get();
        this.faeTier = BloodlinesConfig.FAE_MAX_ARMOR_TIER.get().name();
        this.angelkinTier = BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.get().name();
        this.demonkinTier = BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.get().name();
        this.cooldownSeconds = BloodlinesConfig.PRIMARY_ABILITY_COOLDOWN_SECONDS.get();
        this.durationSeconds = BloodlinesConfig.PRIMARY_ABILITY_DURATION_SECONDS.get();
        this.secondaryCooldownSeconds = BloodlinesConfig.SECONDARY_ABILITY_COOLDOWN_SECONDS.get();
        this.secondaryDurationSeconds = BloodlinesConfig.SECONDARY_ABILITY_DURATION_SECONDS.get();
        this.orbRarityMultiplier = BloodlinesConfig.ORB_SPAWN_RARITY_MULTIPLIER.get();
    }

    private SyncBloodlinesConfigPacket(int foodLevel, double exhaustion, double flySpeed,
            String faeTier, String angelkinTier, String demonkinTier, int cooldownSeconds, int durationSeconds,
            int secondaryCooldownSeconds, int secondaryDurationSeconds, double orbRarityMultiplier)
    {
        this.foodLevel = foodLevel;
        this.exhaustion = exhaustion;
        this.flySpeed = flySpeed;
        this.faeTier = faeTier;
        this.angelkinTier = angelkinTier;
        this.demonkinTier = demonkinTier;
        this.cooldownSeconds = cooldownSeconds;
        this.durationSeconds = durationSeconds;
        this.secondaryCooldownSeconds = secondaryCooldownSeconds;
        this.secondaryDurationSeconds = secondaryDurationSeconds;
        this.orbRarityMultiplier = orbRarityMultiplier;
    }

    public static void encode(SyncBloodlinesConfigPacket msg, FriendlyByteBuf buf)
    {
        buf.writeVarInt(msg.foodLevel);
        buf.writeDouble(msg.exhaustion);
        buf.writeDouble(msg.flySpeed);
        buf.writeUtf(msg.faeTier);
        buf.writeUtf(msg.angelkinTier);
        buf.writeUtf(msg.demonkinTier);
        buf.writeVarInt(msg.cooldownSeconds);
        buf.writeVarInt(msg.durationSeconds);
        buf.writeVarInt(msg.secondaryCooldownSeconds);
        buf.writeVarInt(msg.secondaryDurationSeconds);
        buf.writeDouble(msg.orbRarityMultiplier);
    }

    public static SyncBloodlinesConfigPacket decode(FriendlyByteBuf buf)
    {
        return new SyncBloodlinesConfigPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readDouble());
    }

    public static void handle(SyncBloodlinesConfigPacket msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
        {
            BloodlinesConfig.FAE_REQUIRED_FOOD_LEVEL.set(msg.foodLevel);
            BloodlinesConfig.FAE_EXHAUSTION_PER_BOOST_TICK.set(msg.exhaustion);
            BloodlinesConfig.FAE_FLYING_SPEED.set(msg.flySpeed);
            BloodlinesConfig.MaxArmorTier.tryParse(msg.faeTier).ifPresent(BloodlinesConfig.FAE_MAX_ARMOR_TIER::set);
            BloodlinesConfig.MaxArmorTier.tryParse(msg.angelkinTier).ifPresent(BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER::set);
            BloodlinesConfig.MaxArmorTier.tryParse(msg.demonkinTier).ifPresent(BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER::set);
            BloodlinesConfig.PRIMARY_ABILITY_COOLDOWN_SECONDS.set(msg.cooldownSeconds);
            BloodlinesConfig.PRIMARY_ABILITY_DURATION_SECONDS.set(msg.durationSeconds);
            BloodlinesConfig.SECONDARY_ABILITY_COOLDOWN_SECONDS.set(msg.secondaryCooldownSeconds);
            BloodlinesConfig.SECONDARY_ABILITY_DURATION_SECONDS.set(msg.secondaryDurationSeconds);
            BloodlinesConfig.ORB_SPAWN_RARITY_MULTIPLIER.set(msg.orbRarityMultiplier);
        });
        ctx.setPacketHandled(true);
    }
}
