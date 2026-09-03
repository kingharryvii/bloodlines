package com.harryskingdom.bloodlines.client.race;

import com.harryskingdom.bloodlines.race.RaceAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Client-only clock for the ability cooldown/duration HUD bars (see AbilityCooldownOverlay). Primary and
 * secondary each keep their own independent clock - matches RaceAbilityCooldowns tracking them independently
 * server-side, so using one doesn't touch the other's bar. Both start as if their ability had already been off
 * cooldown forever - a fresh login shouldn't show a half-drained bar - and get reset to "just activated" the
 * moment their own AbilityActivatedPacket arrives.
 */
public final class AbilityHudState
{
    /** Far enough in the past that cooldownProgress()/durationProgress() read as "fully ready" by default. */
    private static final long NEVER_ACTIVATED = Long.MIN_VALUE / 2;

    private static long primaryActivatedAtTick = NEVER_ACTIVATED;
    private static long secondaryActivatedAtTick = NEVER_ACTIVATED;

    private AbilityHudState() {}

    public static void onActivated(boolean secondary)
    {
        if (secondary)
            secondaryActivatedAtTick = clientTick();
        else
            primaryActivatedAtTick = clientTick();
    }

    /** 0 = just used, 1 = fully recharged and ready to use again. */
    public static float cooldownProgress(boolean secondary)
    {
        long elapsed = clientTick() - (secondary ? secondaryActivatedAtTick : primaryActivatedAtTick);
        int cooldownTicks = secondary ? RaceAbility.secondaryCooldownTicks() : RaceAbility.cooldownTicks();
        return Mth.clamp(elapsed / (float) cooldownTicks, 0F, 1F);
    }

    /** 1 = just activated (full duration remaining), 0 = the effect has worn off. */
    public static float durationProgress(boolean secondary)
    {
        long elapsed = clientTick() - (secondary ? secondaryActivatedAtTick : primaryActivatedAtTick);
        int durationTicks = secondary ? RaceAbility.secondaryDurationTicks() : RaceAbility.durationTicks();
        return 1F - Mth.clamp(elapsed / (float) durationTicks, 0F, 1F);
    }

    private static long clientTick()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }
}
