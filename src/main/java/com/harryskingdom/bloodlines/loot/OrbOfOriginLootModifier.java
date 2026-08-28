package com.harryskingdom.bloodlines.loot;

import com.harryskingdom.bloodlines.config.BloodlinesConfig;
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
 * A Global Loot Modifier (not a raw vanilla loot table override) that adds a chance of an Orb of Bloodlines to
 * whichever vanilla loot tables the data file's own "forge:loot_table_id" condition targets - GLMs layer on top
 * of a loot table's normal roll rather than replacing the whole file, so this doesn't conflict with the many
 * other mods in this pack that also touch structure loot. See data/harrys_bloodlines/loot_modifiers/*.json for
 * which structures actually get it and each one's own hand-tuned base chance - one JSON file per structure, all
 * reusing this same class, curated so harder/later-game structures drop it more often.
 * <p>
 * The JSON "chance" is a base rate, not the final roll - BloodlinesConfig.ORB_SPAWN_RARITY_MULTIPLIER scales it
 * at roll time (config is admin-editable in-game; the JSON files are not, short of a datapack reload), so an
 * admin can turn the overall frequency up or down without touching the relative balance between structures.
 */
public class OrbOfOriginLootModifier extends LootModifier
{
    public static final Codec<OrbOfOriginLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance)
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.baseChance))
                    .apply(instance, OrbOfOriginLootModifier::new));

    private final float baseChance;

    public OrbOfOriginLootModifier(LootItemCondition[] conditions, float baseChance)
    {
        super(conditions);
        this.baseChance = baseChance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context)
    {
        float chance = (float) Math.min(1.0, baseChance * BloodlinesConfig.ORB_SPAWN_RARITY_MULTIPLIER.get());
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
