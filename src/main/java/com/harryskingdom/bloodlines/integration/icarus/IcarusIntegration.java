package com.harryskingdom.bloodlines.integration.icarus;

import com.r3x.icarusrewinged.registry.IcarusReItems;
import dev.cammiescorner.icarus.init.IcarusItems;
import dev.cammiescorner.icarus.item.WingItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

/**
 * Soft integration with the Icarus flight mod (via Curios' "back" slot), granting real, sustained flight to
 * Seraph instead of a fake slow-falling effect. Uses Icarus Rewinged's wing textures when that addon is present
 * for a nicer look, falling back to core Icarus wings otherwise. No-ops entirely if Icarus or Curios isn't
 * installed.
 * <p>
 * Fae does NOT use this - their flight is fully native now (see RaceFlightFood), modeled on Medieval Origins
 * Revival's own Pixie race rather than Icarus. Trying to keep both a custom wing render and a hidden real Icarus
 * item for Fae simultaneously (three different suppression techniques: Curios' render toggle, a transparent
 * texture override, and a Mixin cancelling Icarus's own render call) never reliably worked, so Fae is kept
 * entirely independent of Icarus/Curios to remove that whole class of bug.
 */
public final class IcarusIntegration
{
    private static final String BACK_SLOT = "back";
    private static final String RACE_TAG = "BloodlinesRacialWingsRace";
    private static final String SERAPH_RACE_VALUE = "seraph";

    private IcarusIntegration() {}

    public static boolean isLoaded()
    {
        return ModList.get().isLoaded("icarus") && ModList.get().isLoaded("curios");
    }

    private static boolean isRewingedLoaded()
    {
        return ModList.get().isLoaded("icarusrewinged");
    }

    /** Equips the Seraph's big white-and-gold angel wings, rendered normally by Icarus. */
    public static void grantSeraphWings(ServerPlayer player)
    {
        Supplier<? extends Item> wingType = isRewingedLoaded() ? IcarusReItems.FEATHERED_GILDED_WHITE_WINGS : IcarusItems.WHITE_FEATHERED_WINGS;
        grantWings(player, wingType, "Seraph Wings", SERAPH_RACE_VALUE);
    }

    /** Removes the racial wings if the player is currently wearing the pair we gave them. */
    public static void removeWings(ServerPlayer player)
    {
        if (!isLoaded())
            return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findFirstCurio(IcarusIntegration::isRacialWings).ifPresent(result ->
                        handler.getStacksHandler(result.slotContext().identifier())
                                .ifPresent(stacksHandler -> stacksHandler.getStacks().setStackInSlot(result.slotContext().index(), ItemStack.EMPTY))));
    }

    private static void grantWings(ServerPlayer player, Supplier<? extends Item> wingType, String name, String raceTag)
    {
        if (!isLoaded())
            return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
        {
            if (handler.isEquipped(stack -> stack.getItem() instanceof WingItem))
                return;

            handler.getStacksHandler(BACK_SLOT).ifPresent(stacksHandler ->
                    stacksHandler.getStacks().setStackInSlot(0, createWings(wingType, name, raceTag)));
        });
    }

    private static boolean isRacialWings(ItemStack stack)
    {
        return stack.getTag() != null && stack.getTag().contains(RACE_TAG);
    }

    private static ItemStack createWings(Supplier<? extends Item> wingType, String name, String raceTag)
    {
        ItemStack stack = new ItemStack(wingType.get());

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(RACE_TAG, raceTag);
        tag.putBoolean("Unbreakable", true);
        stack.setHoverName(Component.literal(name));

        return stack;
    }
}
