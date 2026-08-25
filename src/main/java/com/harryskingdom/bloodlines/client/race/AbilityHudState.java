package com.harryskingdom.bloodlines.client.race;

import com.harryskingdom.bloodlines.race.RaceAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Client-only clock for the ability cooldown/duration HUD bars (see AbilityCooldownOverlay). Starts as if the
 * ability had already been off cooldown forever - a fresh login shouldn't show a half-drained bar - and gets
 * reset to "just activated" the moment AbilityActivatedPacket arrives.
 */
public final class AbilityHudState
{
    /** Far enough in the past that cooldownProgress()/durationProgress() read as "fully ready" by default. */
    private static long activatedAtTick = Long.MIN_VALUE / 2;

    private AbilityHudState() {}

    public static void onActivated()
    {
        activatedAtTick = clientTick();
    }

    /** 0 = just used, 1 = fully recharged and ready to use again. */
    public static float cooldownProgress()
    {
        long elapsed = clientTick() - activatedAtTick;
        return Mth.clamp(elapsed / (float) RaceAbility.COOLDOWN_TICKS, 0F, 1F);
    }

    /** 1 = just activated (full duration remaining), 0 = the effect has worn off. */
    public static float durationProgress()
    {
        long elapsed = clientTick() - activatedAtTick;
        return 1F - Mth.clamp(elapsed / (float) RaceAbility.DURATION_TICKS, 0F, 1F);
    }

    private static long clientTick()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }
}
