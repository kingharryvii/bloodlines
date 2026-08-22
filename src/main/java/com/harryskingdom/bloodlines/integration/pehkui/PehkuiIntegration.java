package com.harryskingdom.bloodlines.integration.pehkui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * Soft integration with Pehkui (entity scaling), used to make Fae read as small and fairy-like rather than just
 * being a human-sized player with wings — the same trick Medieval Origins Revival uses for its Fae/pixie races.
 * No-ops entirely if Pehkui isn't installed.
 */
public final class PehkuiIntegration
{
    private static final float FAE_SCALE = 0.66f;

    private PehkuiIntegration() {}

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded("pehkui");
    }

    public static void applyFaeScale(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        setScale(ScaleTypes.HEIGHT.getScaleData(player), FAE_SCALE);
        setScale(ScaleTypes.WIDTH.getScaleData(player), FAE_SCALE);
    }

    public static void resetScale(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        ScaleTypes.HEIGHT.getScaleData(player).resetScale();
        ScaleTypes.WIDTH.getScaleData(player).resetScale();
    }

    private static void setScale(ScaleData data, float scale)
    {
        data.setTargetScale(scale);
        data.setPersistence(true);
    }
}
