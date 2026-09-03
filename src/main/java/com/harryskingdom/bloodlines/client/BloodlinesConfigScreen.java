package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.UpdateBloodlinesConfigPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-rolled "Config" screen for the Mods list (no Cloth Config dependency - just Forge's own
 * ConfigScreenHandler.ConfigScreenFactory extension point plus vanilla widgets).
 * <p>
 * Save always goes through UpdateBloodlinesConfigPacket to the server, host and remote op alike - never a
 * direct local .set()/.save() here, even for the singleplayer host. Two reasons:
 * - It has to work that way for a remote op: traced through Forge's real sync code (ConfigSync.receiveSyncedConfig,
 *   ModConfig.acceptSyncedConfig/save), a client connected to someone else's server has its SERVER-type config's
 *   backing object replaced by a plain in-memory CommentedConfig parsed straight from the sync packet, not the
 *   original CommentedFileConfig. ModConfig.save() unconditionally casts to CommentedFileConfig, so calling it on
 *   a synced client throws a ClassCastException outright - there's no built-in mechanism for a client to push
 *   config edits back to a remote server, the sync is one-directional by design.
 * - For the host it's not strictly required (their own BloodlinesConfig.SPEC IS the real file, since
 *   receiveSyncedConfig explicitly skips the replace-with-synced-copy step whenever Minecraft.isLocalServer() is
 *   true) - but sending the packet anyway, over the loopback connection every singleplayer world already uses for
 *   networking, means the fix below covers the host for free instead of needing its own separate code path.
 * The real bug this fixes: a host or op who saved, then reopened the screen, saw their old values again - not
 * because the save failed, but because this screen's rows always read BloodlinesConfig.*.get() directly, and
 * Forge only pushes a SERVER config sync once, at login. Nothing was refreshing that cached copy after a live
 * change, on any client, including the one that made it. UpdateBloodlinesConfigPacket's handler now broadcasts
 * SyncBloodlinesConfigPacket to every connected player after a successful save specifically to close that gap.
 * A player who is neither host nor op gets a read-only view of the same rows instead of a broken Save button.
 * <p>
 * The row list is scrollable - an earlier fixed-offset-from-height/2 layout looked fine at GUI Scale 1 on a
 * large window but overlapped the title at higher scale/smaller windows (this.width/this.height shrink as GUI
 * Scale increases, same class of bug BloodlineSelectScreen already had to account for). Rows are added via
 * addWidget() rather than addRenderableWidget() specifically so their rendering/hit-testing can be driven by
 * this screen's own scroll offset instead of Minecraft's default "always visible" widget handling - toggling a
 * scrolled-out row's `visible` field is enough to disable both, since AbstractWidget.render()/mouseClicked()
 * both check it internally (confirmed by decompiling AbstractWidget, not assumed).
 */
public final class BloodlinesConfigScreen extends Screen
{
    private static final int FIELD_WIDTH = 100;
    private static final int FIELD_HEIGHT = 20;
    private static final int ARMOR_BUTTON_WIDTH = FIELD_WIDTH + 30;
    private static final int FIELD_X_OFFSET = 10;
    private static final int ROW_HEIGHT = 24;
    private static final int LABEL_OFFSET_X = 150;
    private static final int TOP_MARGIN = 40;
    private static final int READONLY_NOTE_Y = 24;
    private static final int BOTTOM_BUTTON_HEIGHT = 20;
    private static final int BOTTOM_SCREEN_MARGIN = 14;
    private static final int VIEWPORT_GAP_ABOVE_BUTTONS = 12;
    private static final int SCROLL_STEP = 16;
    private static final int SCROLLBAR_WIDTH = 3;

    private record Row(AbstractWidget widget, String label, int baseY) {}

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    private EditBox foodLevelBox;
    private EditBox exhaustionBox;
    private EditBox flySpeedBox;
    private EditBox cooldownBox;
    private EditBox durationBox;
    private EditBox orbRarityBox;
    private CycleButton<BloodlinesConfig.MaxArmorTier> faeArmorTierButton;
    private CycleButton<BloodlinesConfig.MaxArmorTier> angelkinArmorTierButton;
    private CycleButton<BloodlinesConfig.MaxArmorTier> demonkinArmorTierButton;

    private int viewportTop;
    private int viewportBottom;
    private int buttonY;
    private int scrollOffset;
    private int scrollMax;

    private Component statusMessage = Component.empty();

    public BloodlinesConfigScreen(Screen parent)
    {
        super(Component.literal("Bloodlines Config"));
        this.parent = parent;
    }

    public static void register()
    {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> new BloodlinesConfigScreen(screen)));
    }

    private static boolean isHost()
    {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    /** Op level 2 - matches the threshold RaceCommands already uses for its own /race admin subcommands. */
    private static boolean isOp()
    {
        var player = Minecraft.getInstance().player;
        return player != null && player.getPermissionLevel() >= 2;
    }

    @Override
    protected void init()
    {
        rows.clear();
        scrollOffset = 0;

        if (!BloodlinesConfig.SPEC.isLoaded())
        {
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(width / 2 - 75, height / 2 + 20, 150, 20).build());
            return;
        }

        boolean editable = isHost() || isOp();
        int fieldX = width / 2 + FIELD_X_OFFSET;

        addRow("Fae required food level", addField(fieldX, String.valueOf(BloodlinesConfig.FAE_REQUIRED_FOOD_LEVEL.get()), editable));
        addRow("Fae exhaustion / boost tick", addField(fieldX, String.valueOf(BloodlinesConfig.FAE_EXHAUSTION_PER_BOOST_TICK.get()), editable));
        addRow("Fae flying speed", addField(fieldX, String.valueOf(BloodlinesConfig.FAE_FLYING_SPEED.get()), editable));
        addRow("Fae max armor tier", addArmorTierButton(fieldX, BloodlinesConfig.FAE_MAX_ARMOR_TIER.get(), editable));
        addRow("Angelkin max armor tier", addArmorTierButton(fieldX, BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.get(), editable));
        addRow("Demonkin max armor tier", addArmorTierButton(fieldX, BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.get(), editable));
        addRow("Ability cooldown (seconds)", addField(fieldX, String.valueOf(BloodlinesConfig.ABILITY_COOLDOWN_SECONDS.get()), editable));
        addRow("Ability duration (seconds)", addField(fieldX, String.valueOf(BloodlinesConfig.ABILITY_DURATION_SECONDS.get()), editable));
        addRow("Orb of Bloodlines spawn rarity", addField(fieldX, String.valueOf(BloodlinesConfig.ORB_SPAWN_RARITY_MULTIPLIER.get()), editable));

        // Fields above are assigned in addRow() call order, matching this declaration order.
        foodLevelBox = (EditBox) rows.get(0).widget();
        exhaustionBox = (EditBox) rows.get(1).widget();
        flySpeedBox = (EditBox) rows.get(2).widget();
        faeArmorTierButton = castArmorButton(rows.get(3).widget());
        angelkinArmorTierButton = castArmorButton(rows.get(4).widget());
        demonkinArmorTierButton = castArmorButton(rows.get(5).widget());
        cooldownBox = (EditBox) rows.get(6).widget();
        durationBox = (EditBox) rows.get(7).widget();
        orbRarityBox = (EditBox) rows.get(8).widget();

        buttonY = height - BOTTOM_BUTTON_HEIGHT - BOTTOM_SCREEN_MARGIN;
        viewportTop = TOP_MARGIN;
        viewportBottom = Math.max(viewportTop, buttonY - VIEWPORT_GAP_ABOVE_BUTTONS);

        int contentHeight = rows.size() * ROW_HEIGHT;
        int viewportHeight = viewportBottom - viewportTop;
        scrollMax = Math.max(0, contentHeight - viewportHeight);
        layoutRows();

        if (editable)
        {
            addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                    .bounds(width / 2 - 105, buttonY, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds(width / 2 + 5, buttonY, 100, 20).build());
        }
        else
        {
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(width / 2 - 75, buttonY, 150, 20).build());
        }
    }

    @SuppressWarnings("unchecked")
    private static CycleButton<BloodlinesConfig.MaxArmorTier> castArmorButton(AbstractWidget widget)
    {
        return (CycleButton<BloodlinesConfig.MaxArmorTier>) widget;
    }

    private void addRow(String label, AbstractWidget widget)
    {
        rows.add(new Row(widget, label, rows.size() * ROW_HEIGHT));
    }

    /** Positions every row from its fixed baseY and the current scrollOffset, and hides rows the scroll has pushed
     *  outside the viewport - AbstractWidget.render()/mouseClicked() both skip a widget once `visible` is false. */
    private void layoutRows()
    {
        for (Row row : rows)
        {
            int y = viewportTop - scrollOffset + row.baseY();
            row.widget().setY(y);
            row.widget().visible = y >= viewportTop && y + FIELD_HEIGHT <= viewportBottom;
        }
    }

    private EditBox addField(int x, String initialValue, boolean editable)
    {
        EditBox box = new EditBox(font, x, 0, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        box.setValue(initialValue);
        box.setEditable(editable);
        return addWidget(box);
    }

    private CycleButton<BloodlinesConfig.MaxArmorTier> addArmorTierButton(int x, BloodlinesConfig.MaxArmorTier initialValue, boolean editable)
    {
        CycleButton<BloodlinesConfig.MaxArmorTier> button = CycleButton.<BloodlinesConfig.MaxArmorTier>builder(tier -> Component.literal(tier.name()))
                .withValues(BloodlinesConfig.MaxArmorTier.values())
                .withInitialValue(initialValue)
                .displayOnlyValue()
                .create(x, 0, ARMOR_BUTTON_WIDTH, FIELD_HEIGHT, Component.empty());
        button.active = editable;
        return addWidget(button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (scrollMax > 0)
        {
            scrollOffset = Mth.clamp((int) (scrollOffset - delta * SCROLL_STEP), 0, scrollMax);
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void save()
    {
        // Belt-and-suspenders - the Save button only exists when editable, but this is exactly the kind of
        // boundary worth checking again right before the action it guards rather than trusting the UI alone.
        if (!isHost() && !isOp())
            return;

        try
        {
            int foodLevel = clamp(Integer.parseInt(foodLevelBox.getValue().trim()), BloodlinesConfig.FAE_FOOD_LEVEL_MIN, BloodlinesConfig.FAE_FOOD_LEVEL_MAX);
            double exhaustion = clamp(Double.parseDouble(exhaustionBox.getValue().trim()), BloodlinesConfig.FAE_EXHAUSTION_MIN, BloodlinesConfig.FAE_EXHAUSTION_MAX);
            double flySpeed = clamp(Double.parseDouble(flySpeedBox.getValue().trim()), BloodlinesConfig.FAE_FLY_SPEED_MIN, BloodlinesConfig.FAE_FLY_SPEED_MAX);
            int cooldown = clamp(Integer.parseInt(cooldownBox.getValue().trim()), BloodlinesConfig.ABILITY_SECONDS_MIN, BloodlinesConfig.ABILITY_COOLDOWN_MAX);
            int duration = clamp(Integer.parseInt(durationBox.getValue().trim()), BloodlinesConfig.ABILITY_SECONDS_MIN, BloodlinesConfig.ABILITY_DURATION_MAX);
            double orbRarity = clamp(Double.parseDouble(orbRarityBox.getValue().trim()), BloodlinesConfig.ORB_RARITY_MULTIPLIER_MIN, BloodlinesConfig.ORB_RARITY_MULTIPLIER_MAX);
            BloodlinesConfig.MaxArmorTier faeTier = faeArmorTierButton.getValue();
            BloodlinesConfig.MaxArmorTier angelkinTier = angelkinArmorTierButton.getValue();
            BloodlinesConfig.MaxArmorTier demonkinTier = demonkinArmorTierButton.getValue();

            // Always over the network, even for the host - see class doc for why: it's not just the only option
            // for a remote op, it's also what makes the post-save broadcast (which fixes stale cached values on
            // every client, including this one) apply uniformly instead of needing a separate host-only path.
            BloodlinesNetwork.CHANNEL.sendToServer(new UpdateBloodlinesConfigPacket(
                    foodLevel, exhaustion, flySpeed, faeTier, angelkinTier, demonkinTier, cooldown, duration, orbRarity));

            onClose();
        }
        catch (NumberFormatException e)
        {
            statusMessage = Component.literal("Enter valid numbers in every field.").withStyle(ChatFormatting.RED);
        }
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        if (!BloodlinesConfig.SPEC.isLoaded())
        {
            graphics.drawCenteredString(font,
                    Component.literal("These settings are stored server-side - join a world or server first."),
                    width / 2, height / 2 - 10, 0xAAAAAA);
        }
        else
        {
            if (!isHost() && !isOp())
                graphics.drawCenteredString(font,
                        Component.literal("Read-only - ask a server op to change these.").withStyle(ChatFormatting.GRAY),
                        width / 2, READONLY_NOTE_Y, 0xAAAAAA);

            int labelX = width / 2 - LABEL_OFFSET_X;
            for (Row row : rows)
            {
                if (!row.widget().visible)
                    continue;

                drawLabel(graphics, row.label(), labelX, row.widget().getY());
                // Rows are added via addWidget(), not addRenderableWidget(), specifically so scrolling can control
                // their visibility - but that also means they're never in Screen's own auto-rendered list, so
                // they have to be drawn by hand here, same as any other manually-managed widget.
                row.widget().render(graphics, mouseX, mouseY, partialTick);
            }

            if (scrollMax > 0)
            {
                int trackX = width / 2 + FIELD_X_OFFSET + ARMOR_BUTTON_WIDTH + 10;
                int viewportHeight = viewportBottom - viewportTop;
                int contentHeight = rows.size() * ROW_HEIGHT;
                graphics.fill(trackX, viewportTop, trackX + SCROLLBAR_WIDTH, viewportBottom, 0x40000000);
                int thumbHeight = Math.max(10, viewportHeight * viewportHeight / contentHeight);
                int thumbY = viewportTop + (viewportHeight - thumbHeight) * scrollOffset / scrollMax;
                graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
            }

            if (!statusMessage.getString().isEmpty())
                graphics.drawCenteredString(font, statusMessage, width / 2, buttonY - 10, 0xFF5555);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawLabel(GuiGraphics graphics, String text, int x, int y)
    {
        graphics.drawString(font, text, x, y + 6, 0xFFFFFF);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }
}
