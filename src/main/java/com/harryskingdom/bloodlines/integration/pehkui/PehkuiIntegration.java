package com.harryskingdom.bloodlines.integration.pehkui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;
import virtuoel.pehkui.util.ScaleUtils;

/**
 * Soft integration with Pehkui (entity scaling), used to make Fae read as small and fairy-like rather than just
 * being a human-sized player with wings — the same trick Medieval Origins Revival uses for its Fae/pixie races.
 * No-ops entirely if Pehkui isn't installed.
 */
public final class PehkuiIntegration
{
    private static final float FAE_SCALE = 0.66f;
    /** Height-only, per the user's own ask - a shorter, stouter silhouette without also narrowing the frame. */
    private static final float DWARF_HEIGHT_SCALE = 0.85f;
    /**
     * Taller AND wider - height-only initially read as too lanky/stretched for "ancient giant"; width now comes
     * along too, but more modestly than height so the shape reads as bulky rather than just uniformly scaled up.
     */
    private static final float TROLL_HEIGHT_SCALE = 1.2f;
    private static final float TROLL_WIDTH_SCALE = 1.15f;

    private PehkuiIntegration() {}

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded("pehkui");
    }

    /**
     * Pehkui shrinks the player's own body model directly (its bones, not an outer PoseStack wrap around the
     * whole entity render), so anything rendered as a separate model in its own RenderLayer - like Fae's wings -
     * doesn't automatically shrink along with it. Callers that need to stay proportional to the current body
     * size (1.0 when Pehkui isn't installed or hasn't scaled this entity) should multiply by this.
     */
    public static float getVisualScale(LivingEntity entity, float partialTicks)
    {
        return isLoaded() ? ScaleUtils.getModelHeightScale(entity, partialTicks) : 1.0F;
    }

    public static void applyFaeScale(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        setScale(ScaleTypes.HEIGHT.getScaleData(player), FAE_SCALE);
        setScale(ScaleTypes.WIDTH.getScaleData(player), FAE_SCALE);
    }

    public static void applyDwarfScale(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        setScale(ScaleTypes.HEIGHT.getScaleData(player), DWARF_HEIGHT_SCALE);
    }

    public static void applyTrollScale(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        setScale(ScaleTypes.HEIGHT.getScaleData(player), TROLL_HEIGHT_SCALE);
        setScale(ScaleTypes.WIDTH.getScaleData(player), TROLL_WIDTH_SCALE);
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
