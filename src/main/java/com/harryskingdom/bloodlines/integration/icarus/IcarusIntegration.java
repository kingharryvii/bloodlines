package com.harryskingdom.bloodlines.integration.icarus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Soft integration with Curios, now used only to strip the real Icarus wing item from any player who was granted
 * one under the old Seraph flight system (before it moved to fully native flight/wings, see
 * SeraphFlightController and SeraphWingsLayer). Neither Fae nor Seraph grant a real Icarus item anymore - Fae's
 * flight was already fully native (see RaceFlightFood), and Seraph's now is too, so this class no longer grants
 * anything, only cleans up leftovers. No-ops entirely if Curios isn't installed.
 */
public final class IcarusIntegration
{
    private static final String RACE_TAG = "BloodlinesRacialWingsRace";

    private IcarusIntegration() {}

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded("curios");
    }

    /** Removes the racial wings if the player is currently wearing a pair we gave them under the old system. */
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
