package com.harryskingdom.bloodlines.integration.icarus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Legacy cleanup only. Angelkin/Demonkin flight no longer grants a real Icarus wing item at all - see
 * IcarusWingHooks, which makes Icarus's own flight code think a player has wings purely off their Bloodlines
 * race, with no physical item anywhere. That replaced an earlier version of
 * this class which DID equip a real item into Curios' "back" slot, which turned out to collide with any other
 * mod also using "back" (backpacks, capes, etc.) - a player couldn't wear both. removeWings here just strips any
 * leftover real item a player might still have equipped from that earlier system, on worlds that predate this
 * migration. No-ops entirely if Curios isn't installed.
 */
public final class IcarusIntegration
{
    private static final String RACE_TAG = "BloodlinesRacialWingsRace";

    private IcarusIntegration() {}

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded("curios");
    }

    /** Removes the racial wings if the player is currently wearing a leftover pair from the old item-based system. */
    public static void removeWings(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findFirstCurio(IcarusIntegration::isRacialWings).ifPresent(result ->
                        handler.getStacksHandler(result.slotContext().identifier())
                                .ifPresent(stacksHandler -> stacksHandler.getStacks().setStackInSlot(result.slotContext().index(), ItemStack.EMPTY))));
    }

    private static boolean isRacialWings(ItemStack stack)
    {
        return stack.getTag() != null && stack.getTag().contains(RACE_TAG);
    }
}
