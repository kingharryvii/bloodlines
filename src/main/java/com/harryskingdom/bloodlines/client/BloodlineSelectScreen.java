package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.SelectRacePacket;
import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.RacePower;
import com.harryskingdom.bloodlines.race.RacePowers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

/**
 * A single scrollable card for one race at a time (icon, tier, flavor text, a strengths/weaknesses power list),
 * cycled with the side arrows or the Select button confirms it - modeled on the real Origins mod's own choose-
 * origin screen layout, at the user's request. Shows every race, including locked ones (with a lock note instead
 * of an unlock button) - the server is already the sole authority on whether a pick is actually allowed
 * (RaceSelection#select via SelectRacePacket), so the client doesn't need to duplicate that logic to decide what
 * to show.
 * <p>
 * The per-race icon used to be a custom AI-generated texture (textures/gui/race_icon/&lt;race&gt;.png) - replaced
 * with a real vanilla item render (see iconItemFor) since those textures were never meant to be final art and
 * the user would rather have a placeholder built from assets that already exist in the game than more AI output.
 * The asset files themselves are left in place under textures/gui/race_icon/ in case real commissioned art
 * replaces them later; nothing in this class references that path anymore.
 */
public class BloodlineSelectScreen extends Screen
{
    // Reserved space around the card for the title above, the Select button below, and the nav arrows to each
    // side - cardWidth/cardHeight are computed from this.width/this.height in init() rather than being fixed
    // pixel constants, since a fixed size that looks right at GUI Scale 1 can badly overflow the screen at a
    // higher scale (this.width/this.height already shrink as scale increases - a "huge screen" bug report at a
    // high GUI Scale setting is exactly what a hardcoded size like 300x320 produces).
    private static final int SIDE_MARGIN = 60;
    private static final int TOP_MARGIN = 50;
    private static final int BOTTOM_MARGIN = 40;
    private static final int MAX_cardWidth = 280;
    private static final int MAX_cardHeight = 260;
    private static final int MIN_cardWidth = 160;
    private static final int MIN_cardHeight = 140;

    private static final int HEADER_HEIGHT = 44;
    private static final int ICON_SIZE = 32;
    private static final int PADDING = 12;
    private static final int POWER_LINE_GAP = 4;
    private static final int SCROLL_STEP = 14;

    private static final int COLOR_BORDER = 0xFFFFFFFF;
    private static final int COLOR_HEADER_BG = 0xFF8B6914;
    private static final int COLOR_BODY_BG = 0xE0303030;
    private static final int COLOR_DESC_TEXT = 0xFFC6C6C6;
    private static final int COLOR_TITLE_POSITIVE = 0xFF55FFFF;
    private static final int COLOR_TITLE_NEGATIVE = 0xFFFF5555;
    private static final int COLOR_TITLE_NEUTRAL = 0xFF55FFFF;
    private static final int COLOR_SYMBOL_POSITIVE = 0xFF55FF55;
    private static final int COLOR_SYMBOL_NEGATIVE = 0xFFFF5555;
    private static final int COLOR_SYMBOL_NEUTRAL = 0xFF55FFFF;

    // Dragonborn has no real kit yet (see RaceStats/RacePowers) and its unlock path isn't built - it'll come
    // back once that's a real, playable bloodline rather than a placeholder. Excluded by name rather than by
    // "locked" in general, since future unlockable races should still show up here once they're actually finished.
    private final List<Race> races = Arrays.stream(Race.values()).filter(race -> race != Race.DRAGONBORN).toList();
    private int currentIndex;
    private int scrollOffset;
    private int scrollMax;

    private int cardX;
    private int cardY;
    private int cardWidth;
    private int cardHeight;

    public BloodlineSelectScreen()
    {
        super(Component.literal("Choose Your Bloodline"));
    }

    @Override
    protected void init()
    {
        super.init();

        cardWidth = Math.max(MIN_cardWidth, Math.min(MAX_cardWidth, this.width - SIDE_MARGIN * 2));
        cardHeight = Math.max(MIN_cardHeight, Math.min(MAX_cardHeight, this.height - TOP_MARGIN - BOTTOM_MARGIN));
        cardX = (this.width - cardWidth) / 2;
        cardY = (this.height - cardHeight - BOTTOM_MARGIN) / 2 + 10;

        int arrowY = cardY + cardHeight / 2 - 10;
        int leftArrowX = Math.max(4, cardX - 30);
        int rightArrowX = Math.min(this.width - 24, cardX + cardWidth + 10);

        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1))
                .bounds(leftArrowX, arrowY, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1))
                .bounds(rightArrowX, arrowY, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Select"), b -> selectCurrent())
                .bounds(cardX + cardWidth / 2 - 50, cardY + cardHeight + 12, 100, 20).build());
    }

    private void cycle(int direction)
    {
        currentIndex = Math.floorMod(currentIndex + direction, races.size());
        scrollOffset = 0;
    }

    private void selectCurrent()
    {
        BloodlinesNetwork.CHANNEL.sendToServer(new SelectRacePacket(races.get(currentIndex)));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);

        Race race = races.get(currentIndex);

        graphics.drawCenteredString(this.font, Component.literal("CHOOSE YOUR BLOODLINE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, cardY - 24, 0xFFFFFF);

        drawCardFrame(graphics, race);
        drawHeader(graphics, race);
        int bodyTop = drawDescription(graphics, race);
        bodyTop = drawArmorNote(graphics, race, bodyTop);
        drawPowerList(graphics, race, bodyTop, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCardFrame(GuiGraphics graphics, Race race)
    {
        graphics.fill(cardX - 2, cardY - 2, cardX + cardWidth + 2, cardY + cardHeight + 2, COLOR_BORDER);
        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, COLOR_BODY_BG);
    }

    private void drawHeader(GuiGraphics graphics, Race race)
    {
        int headerRight = cardX + cardWidth;
        graphics.fill(cardX, cardY, headerRight, cardY + HEADER_HEIGHT, COLOR_HEADER_BG);

        int iconX = cardX + PADDING;
        int iconY = cardY + (HEADER_HEIGHT - ICON_SIZE) / 2;
        drawIcon(graphics, race, iconX, iconY);

        int nameX = iconX + ICON_SIZE + 8;
        graphics.drawString(this.font, Component.literal(race.getDisplayName()).withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE),
                nameX, cardY + HEADER_HEIGHT / 2 - 9, 0xFFFFFF);
        if (race.isLocked())
            graphics.drawString(this.font, Component.literal("LOCKED").withStyle(ChatFormatting.RED),
                    nameX, cardY + HEADER_HEIGHT / 2 + 2, 0xFFFFFF);

        drawTierPips(graphics, race, headerRight - PADDING, cardY + HEADER_HEIGHT / 2);
    }

    private void drawIcon(GuiGraphics graphics, Race race, int x, int y)
    {
        ItemStack icon = new ItemStack(iconItemFor(race));
        float scale = (float) ICON_SIZE / 16F;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0F);
        graphics.pose().scale(scale, scale, 1F);
        graphics.renderFakeItem(icon, 0, 0);
        graphics.pose().popPose();
    }

    /**
     * A vanilla item standing in for real per-race art - picked for thematic fit with each race's flavor and
     * kit (Wood Elf's bow damage bonus, Dwarf's mining speed, Merfolk's trident, etc.), not just "closest visual
     * match", so the icon still says something true about the race even as a placeholder.
     */
    private static Item iconItemFor(Race race)
    {
        return switch (race)
        {
            case HUMAN -> Items.IRON_SWORD;
            case WOOD_ELF -> Items.BOW;
            case HIGH_ELF -> Items.ENCHANTED_BOOK;
            case MOON_ELF -> Items.ENDER_PEARL;
            case DWARF -> Items.IRON_PICKAXE;
            case FAE -> Items.FEATHER;
            case GOBLIN -> Items.GOLD_NUGGET;
            case DRAGONBORN -> Items.DRAGON_EGG;
            case BEASTKIN -> Items.RABBIT_FOOT;
            case REVENANT -> Items.WITHER_ROSE;
            case GHOUL -> Items.ROTTEN_FLESH;
            case DEMON -> Items.BLAZE_POWDER;
            case TROLL -> Items.MOSS_BLOCK;
            case MERFOLK -> Items.TRIDENT;
            case SERAPH -> Items.ELYTRA;
        };
    }

    private void drawTierPips(GuiGraphics graphics, Race race, int rightEdgeX, int centerY)
    {
        int filled = switch (race.getTier())
        {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
        };
        int pipSize = 6;
        int gap = 4;
        int startX = rightEdgeX - (pipSize * 3 + gap * 2);
        for (int i = 0; i < 3; i++)
        {
            int x = startX + i * (pipSize + gap);
            int color = i < filled ? 0xFFFFEE55 : 0xFF555555;
            graphics.fill(x, centerY - pipSize / 2, x + pipSize, centerY + pipSize / 2, 0xFF000000);
            graphics.fill(x + 1, centerY - pipSize / 2 + 1, x + pipSize - 1, centerY + pipSize / 2 - 1, color);
        }
    }

    /** Draws the flavor description below the header and returns the y coordinate the scrollable body should start at. */
    private int drawDescription(GuiGraphics graphics, Race race)
    {
        int textX = cardX + PADDING;
        int maxWidth = cardWidth - PADDING * 2;
        int y = cardY + HEADER_HEIGHT + PADDING;

        List<FormattedCharSequence> lines = this.font.split(FormattedText.of(race.getDescription()), maxWidth);
        for (FormattedCharSequence line : lines)
        {
            graphics.drawString(this.font, line, textX, y, COLOR_DESC_TEXT);
            y += this.font.lineHeight + 1;
        }

        return y + PADDING / 2;
    }

    /**
     * A short, always-visible caveat (unlike the scrollable power list below it, which a player may never scroll
     * down to) for the winged races whose flight comes with an armor-weight cap. Kept deliberately generic
     * rather than naming a specific material - the actual cap is admin-configurable (see BloodlinesConfig /
     * RaceEffectEvents), so a hardcoded "no heavier than chainmail" claim here would go stale the moment a server
     * changes it.
     */
    private int drawArmorNote(GuiGraphics graphics, Race race, int y)
    {
        if (race != Race.SERAPH && race != Race.DEMON && race != Race.FAE)
            return y;

        graphics.drawString(this.font, Component.literal("⚠ Armor restrictions may apply").withStyle(ChatFormatting.YELLOW),
                cardX + PADDING, y, 0xFFFFFF);
        return y + this.font.lineHeight + PADDING / 2;
    }

    private void drawPowerList(GuiGraphics graphics, Race race, int bodyTop, int mouseX, int mouseY)
    {
        int textX = cardX + PADDING;
        int textWidth = cardWidth - PADDING * 2 - 6;
        int bodyBottom = cardY + cardHeight - PADDING;

        List<RacePower> powers = RacePowers.of(race);

        graphics.enableScissor(cardX, bodyTop, cardX + cardWidth, bodyBottom);

        int y = bodyTop - scrollOffset;
        for (RacePower power : powers)
        {
            int symbolColor = switch (power.category())
            {
                case POSITIVE -> COLOR_SYMBOL_POSITIVE;
                case NEGATIVE -> COLOR_SYMBOL_NEGATIVE;
                case NEUTRAL -> COLOR_SYMBOL_NEUTRAL;
            };
            String symbol = switch (power.category())
            {
                case POSITIVE -> "+";
                case NEGATIVE -> "-";
                case NEUTRAL -> "*";
            };
            int titleColor = power.category() == RacePower.Category.NEGATIVE ? COLOR_TITLE_NEGATIVE : COLOR_TITLE_POSITIVE;

            graphics.drawString(this.font, symbol, textX, y, symbolColor);
            graphics.drawString(this.font, Component.literal(power.title()).withStyle(ChatFormatting.BOLD),
                    textX + 10, y, titleColor);
            y += this.font.lineHeight + 1;

            List<FormattedCharSequence> descLines = this.font.split(FormattedText.of(power.description()), textWidth - 10);
            for (FormattedCharSequence line : descLines)
            {
                graphics.drawString(this.font, line, textX + 10, y, COLOR_DESC_TEXT);
                y += this.font.lineHeight;
            }

            y += POWER_LINE_GAP;
        }

        graphics.disableScissor();

        int contentHeight = y - (bodyTop - scrollOffset);
        int visibleHeight = bodyBottom - bodyTop;
        scrollMax = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.min(scrollOffset, scrollMax);

        if (scrollMax > 0)
        {
            int trackX = cardX + cardWidth - 4;
            graphics.fill(trackX, bodyTop, trackX + 3, bodyBottom, 0xFF1A1A1A);
            int thumbHeight = Math.max(10, visibleHeight * visibleHeight / contentHeight);
            int thumbY = bodyTop + (visibleHeight - thumbHeight) * scrollOffset / scrollMax;
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (scrollMax > 0)
        {
            scrollOffset = (int) Math.max(0, Math.min(scrollMax, scrollOffset - delta * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
