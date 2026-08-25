package com.harryskingdom.bloodlines.loot;

import com.harryskingdom.bloodlines.item.BloodlinesItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * A Global Loot Modifier (not a raw vanilla loot table override) that adds a chance of an Orb of Origin to
 * whichever vanilla loot tables the data file's own "forge:loot_table_id" condition targets - GLMs layer on top
 * of a loot table's normal roll rather than replacing the whole file, so this doesn't conflict with the many
 * other mods in this pack that also touch structure loot. See data/harrys_bloodlines/loot_modifiers/*.json for
 * which structures actually get it and at what chance - one JSON file per structure, all reusing this same class.
 */
public class OrbOfOriginLootModifier extends LootModifier
{
    public static final Codec<OrbOfOriginLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance)
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .apply(instance, OrbOfOriginLootModifier::new));

    private final float chance;

    public OrbOfOriginLootModifier(LootItemCondition[] conditions, float chance)
    {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        if (context.getRandom().nextFloat() < chance)
            generatedLoot.add(new ItemStack(BloodlinesItems.ORB_OF_ORIGIN.get()));

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec()
    {
        return BloodlinesLootModifiers.ORB_OF_ORIGIN.get();
    }
}
