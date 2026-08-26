package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.client.race.AbilityHudState;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.RaceAbility;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Two small bars next to the hotbar for the local player's racial ability, at the user's request - modeled on
 * Medieval Origins Revival's own ability HUD. The top bar is the cooldown: empty the instant you use it, fills
 * left-to-right as RaceAbility.cooldownTicks() elapses, full again once ready. The bottom bar is the active
 * duration: full the instant you use it, drains to empty over RaceAbility.durationTicks() - only relevant while
 * the ability's effects are actually still ticking, so it's hidden once it hits zero rather than sitting there
 * permanently empty.
 */
public final class AbilityCooldownOverlay implements IGuiOverlay
{
    private static final int BAR_WIDTH = 90;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_GAP = 2;
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int RIGHT_GAP = 8;

    private static final int COLOR_BORDER = 0xFF000000;
    private static final int COLOR_BG = 0xB0202020;
    private static final int COLOR_COOLDOWN_FILL = 0xFFDDB84A;
    private static final int COLOR_DURATION_FILL = 0xFF4AB8DD;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui)
            return;

        Race race = ClientRaceCache.get(player.getId());
        if (race == null)
            return;

        String abilityName = RaceAbility.nameFor(race);
        if (abilityName == null)
            return;

        int x = screenWidth / 2 + HOTBAR_HALF_WIDTH + RIGHT_GAP;
        int y = screenHeight - 29;

        RenderSystem.enableBlend();

        graphics.drawString(mc.font, abilityName, x, y - mc.font.lineHeight - 2, 0xFFFFFFFF);

        float cooldown = AbilityHudState.cooldownProgress();
        drawBar(graphics, x, y, cooldown, COLOR_COOLDOWN_FILL);

        float duration = AbilityHudState.durationProgress();
        if (duration > 0F)
            drawBar(graphics, x, y + BAR_HEIGHT + BAR_GAP, duration, COLOR_DURATION_FILL);

        RenderSystem.disableBlend();
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, float progress, int fillColor)
    {
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BORDER);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_BG);

        int filled = Math.round(BAR_WIDTH * progress);
        if (filled > 0)
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, fillColor);
    }
}
