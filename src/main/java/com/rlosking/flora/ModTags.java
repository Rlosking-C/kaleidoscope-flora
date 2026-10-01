package com.rlosking.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Block and item tags owned by Kaleidoscope Flora.
 */
public final class ModTags {

    /**
     * Thin "sheet" ground-cover blocks the Springtime Stroll drink may lay
     * underfoot while its holder walks: wool carpets, snow layers, pink
     * petals, wildflowers, leaf litter... Data-driven on purpose so datapacks
     * and other mods can extend the palette freely.
     */
    public static final TagKey<Block> SHEET_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "sheet_blocks"));

    /**
     * Every flower the Raw Flower Cake accepts (v0.3.4).
     *
     * <p>Matches the flower list of {@link FloraTeas} - the 22 vanilla flowers
     * plus the four VanillaBackport ones, which the tag marks
     * {@code "required": false} so the file still loads when VB is absent.</p>
     *
     * <p><b>The list has to live in a data file, not in code.</b> The recipe
     * says "any flower" as a single ingredient, and an ingredient can only
     * point at a tag; enumerating 26 items into 26 recipes would also freeze
     * the set, so a datapack could no longer add a flower without a code
     * change. {@code minecraft:gold_nugget} - the stand-in for the not yet
     * ported golden dandelion - is deliberately <b>not</b> in the tag: it is
     * not a flower, and a cake made of gold would be a strange thing to add to
     * the food chain by accident.</p>
     */
    public static final TagKey<Item> CAKE_FLOWERS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "cake_flowers"));

    private ModTags() {
    }
}
