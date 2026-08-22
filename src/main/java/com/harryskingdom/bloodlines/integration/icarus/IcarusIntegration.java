package com.harryskingdom.bloodlines.integration.icarus;

import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Real integration with Icarus + Curios: Fae and Seraph get an actual Icarus wing item auto-equipped into their
 * Curios "back" slot on race selection, so Icarus's own mod code (the real flight trigger, stamina, boost,
 * loop-de-loop, wing durability - all of it) runs completely natively, exactly as it would for any player who
 * equipped wings themselves. No custom flight code of our own is needed at all.
 * <p>
 * Seraph shows Icarus's own white Feathered Wings render, fully visible, animated by Icarus itself. Fae keeps
 * its own custom-modeled wings (RaceWingModel/RacialWingsLayer) instead: the purple Feathered Wings item here is
 * still equipped (Fae needs it for Icarus's flight mechanics same as Seraph), but its render is suppressed via
 * IcarusAPIClient.addRenderPredicate (see BloodlinesClientSetup) so only the custom wings show.
 * <p>
 * No-ops entirely if Icarus or Curios isn't loaded.
 */
public final class IcarusIntegration
{
    private static final String SLOT = "back";
    private static final ResourceLocation FAE_WINGS = new ResourceLocation("icarus", "purple_feathered_wings");
    private static final ResourceLocation SERAPH_WINGS = new ResourceLocation("icarus", "white_feathered_wings");

    private IcarusIntegration() {}

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded("icarus") && ModList.get().isLoaded("curios");
    }

    /** Pass the player's current race, or null if they no longer have a flying race. */
    public static void updateWings(ServerPlayer player, Race race)
    {
        if (!isLoaded())
            return;

        ResourceLocation wingsId = race == Race.FAE ? FAE_WINGS : race == Race.SERAPH ? SERAPH_WINGS : null;

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> handler.getStacksHandler(SLOT).ifPresent(stacksHandler ->
        {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            ItemStack current = stacks.getStackInSlot(0);

            if (wingsId == null)
            {
                if (isIcarusWings(current))
                    stacks.setStackInSlot(0, ItemStack.EMPTY);
                return;
            }

            Item wingItem = ForgeRegistries.ITEMS.getValue(wingsId);
            if (wingItem == null || current.is(wingItem))
                return;

            stacks.setStackInSlot(0, new ItemStack(wingItem));
            stacksHandler.update();
        }));
    }

    private static boolean isIcarusWings(ItemStack stack)
    {
        if (stack.isEmpty())
            return false;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("icarus");
    }
}
