package com.rlosking.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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

    private ModTags() {
    }
}
