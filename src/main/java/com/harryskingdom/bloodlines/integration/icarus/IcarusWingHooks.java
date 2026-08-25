package com.harryskingdom.bloodlines.integration.icarus;

import com.harryskingdom.bloodlines.race.Race;
import com.harryskingdom.bloodlines.race.WingedRace;
import dev.cammiescorner.icarus.init.IcarusItems;
import dev.cammiescorner.icarus.util.IcarusHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The real fix for Angelkin/Demonkin flight and wing rendering - traced Medieval Origins Revival's own Valkyrie
 * integration (their IcarusHelperMixin) for the technique: never equip a real Icarus item into any Curios slot
 * at all (that's what used to collide with backpacks/capes on the shared "back" slot). Instead, make Icarus
 * itself believe a winged race has wings.
 * <p>
 * A first attempt did this with a Mixin on IcarusHelper's hasWings()/getEquippedWings() methods, but Icarus's own
 * internal code - both the Forge flight-trigger event handler (dev.cammiescorner.icarus.forge.EventHandler,
 * confirmed via javap on the real jar, toggles a Caelus attribute based on hasWings()) and the client wing
 * renderer - turned out to sometimes read IcarusHelper's PUBLIC, MUTABLE fields directly
 * (IcarusHelper.hasWings.test(entity), not IcarusHelper.hasWings(entity)), which a method-level Mixin never sees.
 * Wrapping the fields themselves fixes both: every caller, whether it goes through the method wrapper or reads
 * the field directly, ends up consulting the same replaced function object. No Mixin, no build tooling, no risk
 * of the transformation silently failing to apply - this either runs or it doesn't.
 * <p>
 * Must run after Icarus has set its own real values into these fields (its own platform bootstrap), or our wrap
 * gets immediately overwritten - see install(), called from Bloodlines' own FMLCommonSetupEvent, ordered after
 * Icarus's via mods.toml's existing "ordering=AFTER" on the icarus dependency.
 */
public final class IcarusWingHooks
{
    private IcarusWingHooks() {}

    public static void install()
    {
        if (!ModList.get().isLoaded("icarus"))
            return;

        Predicate<LivingEntity> originalHasWings = IcarusHelper.hasWings;
        IcarusHelper.hasWings = entity -> WingedRace.isWinged(entity) || originalHasWings.test(entity);

        Function<LivingEntity, ItemStack> originalGetEquippedWings = IcarusHelper.getEquippedWings;
        IcarusHelper.getEquippedWings = entity ->
        {
            Race race = WingedRace.raceOf(entity);
            if (race == null)
                return originalGetEquippedWings.apply(entity);

            Supplier<? extends Item> wingType = race == Race.SERAPH ? IcarusItems.WHITE_FEATHERED_WINGS : IcarusItems.RED_DRAGON_WINGS;
            return new ItemStack(wingType.get());
        };
    }
}
