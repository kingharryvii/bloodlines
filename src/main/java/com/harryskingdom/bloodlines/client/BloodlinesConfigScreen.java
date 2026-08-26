package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Hand-rolled "Config" screen for the Mods list (no Cloth Config dependency - just Forge's own
 * ConfigScreenHandler.ConfigScreenFactory extension point plus vanilla widgets).
 * <p>
 * Editing is restricted to Minecraft.hasSingleplayerServer() (true only for singleplayer, or for you
 * specifically if you opened to LAN) - NOT a permission-level check, a "can this even work" check. Traced
 * through Forge's real sync code (ConfigSync.receiveSyncedConfig, ModConfig.acceptSyncedConfig/save): a client
 * connected to someone else's server has its SERVER-type config's backing object replaced by a plain in-memory
 * CommentedConfig parsed straight from the sync packet, not the original CommentedFileConfig. ModConfig.save()
 * unconditionally casts to CommentedFileConfig - so calling it as a synced (non-host) client throws a
 * ClassCastException, not just "shouldn't be allowed", it fails outright. There is no built-in mechanism for a
 * client to push config edits back to a remote dedicated server; the sync is one-directional by design. So a
 * non-host player gets a read-only view of the same rows instead of a broken Save button.
 */
public final class BloodlinesConfigScreen extends Screen
{
    private static final int FIELD_WIDTH = 100;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    private static final int LABEL_OFFSET_X = 150;

    // Must mirror the ranges given to BloodlinesConfig's defineInRange() calls - ForgeConfigSpec doesn't expose
    // a value's own valid range back out, so client-side clamping duplicates them here on purpose.
    private static final int FOOD_LEVEL_MIN = 0, FOOD_LEVEL_MAX = 20;
    private static final double EXHAUSTION_MIN = 0.0, EXHAUSTION_MAX = 1.0;
    private static final double FLY_SPEED_MIN = 0.005, FLY_SPEED_MAX = 0.5;
    private static final int SECONDS_MIN = 1, COOLDOWN_MAX = 3600, DURATION_MAX = 600;

    private final Screen parent;

    private EditBox foodLevelBox;
    private EditBox exhaustionBox;
    private EditBox flySpeedBox;
    private EditBox cooldownBox;
    private EditBox durationBox;
    private CycleButton<BloodlinesConfig.MaxArmorTier> faeArmorTierButton;
    private CycleButton<BloodlinesConfig.MaxArmorTier> angelkinArmorTierButton;
    private CycleButton<BloodlinesConfig.MaxArmorTier> demonkinArmorTierButton;

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

    @Override
    protected void init()
    {
        if (!BloodlinesConfig.SPEC.isLoaded())
        {
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(width / 2 - 75, height / 2 + 20, 150, 20).build());
            return;
        }

        boolean editable = isHost();

        int fieldX = width / 2 + 10;
        int y = height / 2 - 134;

        foodLevelBox = addField(fieldX, y, String.valueOf(BloodlinesConfig.FAE_REQUIRED_FOOD_LEVEL.get()), editable);
        y += ROW_HEIGHT;
        exhaustionBox = addField(fieldX, y, String.valueOf(BloodlinesConfig.FAE_EXHAUSTION_PER_BOOST_TICK.get()), editable);
        y += ROW_HEIGHT;
        flySpeedBox = addField(fieldX, y, String.valueOf(BloodlinesConfig.FAE_FLYING_SPEED.get()), editable);
        y += ROW_HEIGHT;

        faeArmorTierButton = addArmorTierButton(fieldX, y, BloodlinesConfig.FAE_MAX_ARMOR_TIER.get(), editable);
        y += ROW_HEIGHT;
        angelkinArmorTierButton = addArmorTierButton(fieldX, y, BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.get(), editable);
        y += ROW_HEIGHT;
        demonkinArmorTierButton = addArmorTierButton(fieldX, y, BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.get(), editable);
        y += ROW_HEIGHT;

        cooldownBox = addField(fieldX, y, String.valueOf(BloodlinesConfig.ABILITY_COOLDOWN_SECONDS.get()), editable);
        y += ROW_HEIGHT;
        durationBox = addField(fieldX, y, String.valueOf(BloodlinesConfig.ABILITY_DURATION_SECONDS.get()), editable);
        y += ROW_HEIGHT + 16;

        if (editable)
        {
            addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                    .bounds(width / 2 - 105, y, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                    .bounds(width / 2 + 5, y, 100, 20).build());
        }
        else
        {
            addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(width / 2 - 75, y, 150, 20).build());
        }
    }

    private EditBox addField(int x, int y, String initialValue, boolean editable)
    {
        EditBox box = new EditBox(font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        box.setValue(initialValue);
        box.setEditable(editable);
        return addRenderableWidget(box);
    }

    private CycleButton<BloodlinesConfig.MaxArmorTier> addArmorTierButton(int x, int y, BloodlinesConfig.MaxArmorTier initialValue, boolean editable)
    {
        CycleButton<BloodlinesConfig.MaxArmorTier> button = addRenderableWidget(
                CycleButton.<BloodlinesConfig.MaxArmorTier>builder(tier -> Component.literal(tier.name()))
                        .withValues(BloodlinesConfig.MaxArmorTier.values())
                        .withInitialValue(initialValue)
                        .displayOnlyValue()
                        .create(x, y, FIELD_WIDTH + 30, FIELD_HEIGHT, Component.empty()));
        button.active = editable;
        return button;
    }

    private void save()
    {
        // Belt-and-suspenders - the Save button only exists when editable, but this is exactly the kind of
        // boundary worth checking again right before the action it guards rather than trusting the UI alone.
        if (!isHost())
            return;

        try
        {
            int foodLevel = clamp(Integer.parseInt(foodLevelBox.getValue().trim()), FOOD_LEVEL_MIN, FOOD_LEVEL_MAX);
            double exhaustion = clamp(Double.parseDouble(exhaustionBox.getValue().trim()), EXHAUSTION_MIN, EXHAUSTION_MAX);
            double flySpeed = clamp(Double.parseDouble(flySpeedBox.getValue().trim()), FLY_SPEED_MIN, FLY_SPEED_MAX);
            int cooldown = clamp(Integer.parseInt(cooldownBox.getValue().trim()), SECONDS_MIN, COOLDOWN_MAX);
            int duration = clamp(Integer.parseInt(durationBox.getValue().trim()), SECONDS_MIN, DURATION_MAX);

            BloodlinesConfig.FAE_REQUIRED_FOOD_LEVEL.set(foodLevel);
            BloodlinesConfig.FAE_EXHAUSTION_PER_BOOST_TICK.set(exhaustion);
            BloodlinesConfig.FAE_FLYING_SPEED.set(flySpeed);
            BloodlinesConfig.FAE_MAX_ARMOR_TIER.set(faeArmorTierButton.getValue());
            BloodlinesConfig.ANGELKIN_MAX_ARMOR_TIER.set(angelkinArmorTierButton.getValue());
            BloodlinesConfig.DEMONKIN_MAX_ARMOR_TIER.set(demonkinArmorTierButton.getValue());
            BloodlinesConfig.ABILITY_COOLDOWN_SECONDS.set(cooldown);
            BloodlinesConfig.ABILITY_DURATION_SECONDS.set(duration);
            BloodlinesConfig.SPEC.save();

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
            if (!isHost())
                graphics.drawCenteredString(font,
                        Component.literal("Read-only - only the server host can change these.").withStyle(ChatFormatting.GRAY),
                        width / 2, height / 2 - 152, 0xAAAAAA);

            int labelX = width / 2 - LABEL_OFFSET_X;
            int y = height / 2 - 134;
            drawLabel(graphics, "Fae required food level", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Fae exhaustion / boost tick", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Fae flying speed", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Fae max armor tier", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Angelkin max armor tier", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Demonkin max armor tier", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Ability cooldown (seconds)", labelX, y);
            y += ROW_HEIGHT;
            drawLabel(graphics, "Ability duration (seconds)", labelX, y);

            if (!statusMessage.getString().isEmpty())
                graphics.drawCenteredString(font, statusMessage, width / 2, height / 2 + 140, 0xFF5555);
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
