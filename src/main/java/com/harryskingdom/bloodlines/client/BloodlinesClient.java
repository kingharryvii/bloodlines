package com.harryskingdom.bloodlines.client;

import net.minecraft.client.Minecraft;

public final class BloodlinesClient
{
    private BloodlinesClient() {}

    public static void openBloodlineScreen()
    {
        Minecraft.getInstance().setScreen(new BloodlineSelectScreen());
    }
}
