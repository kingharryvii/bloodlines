package com.harryskingdom.bloodlines.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class BloodlinesKeyMappings
{
    public static final String CATEGORY = "key.categories.harrys_bloodlines";

    public static final KeyMapping USE_PRIMARY_ABILITY = new KeyMapping(
            "key.harrys_bloodlines.use_primary_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H),
            CATEGORY
    );

    public static final KeyMapping USE_SECONDARY_ABILITY = new KeyMapping(
            "key.harrys_bloodlines.use_secondary_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G),
            CATEGORY
    );

    private BloodlinesKeyMappings() {}
}
