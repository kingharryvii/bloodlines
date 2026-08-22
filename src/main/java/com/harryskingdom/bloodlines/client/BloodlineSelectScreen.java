package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.SelectRacePacket;
import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BloodlineSelectScreen extends Screen
{
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLUMNS = 3;
    private static final int PADDING = 10;

    private final List<Race> races = Race.startingRaces();
    private final int rows = (races.size() + COLUMNS - 1) / COLUMNS;
    private final int gridHeight = rows * BUTTON_HEIGHT + (rows - 1) * PADDING;

    public BloodlineSelectScreen()
    {
        super(Component.literal("Choose Your Bloodline"));
    }

    private int gridStartY()
    {
        return (this.height - gridHeight) / 2 + 10;
    }

    @Override
    protected void init()
    {
        super.init();

        int totalWidth = COLUMNS * BUTTON_WIDTH + (COLUMNS - 1) * PADDING;
        int startX = (this.width - totalWidth) / 2;
        int startY = gridStartY();

        for (int i = 0; i < races.size(); i++)
        {
            Race race = races.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * (BUTTON_WIDTH + PADDING);
            int y = startY + row * (BUTTON_HEIGHT + PADDING);

            this.addRenderableWidget(Button.builder(Component.literal(race.getDisplayName()), button -> selectRace(race))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.literal(race.getDescription())))
                    .build());
        }
    }

    private void selectRace(Race race)
    {
        BloodlinesNetwork.CHANNEL.sendToServer(new SelectRacePacket(race));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int startY = gridStartY();
        graphics.drawCenteredString(this.font, Component.literal("CHOOSE YOUR BLOODLINE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, startY - 35, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Some bloodlines must be earned.").withStyle(ChatFormatting.GRAY),
                this.width / 2, startY + gridHeight + 15, 0xAAAAAA);
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
