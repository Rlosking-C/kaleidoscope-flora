package com.rlosking.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {

    public static final TagKey<Block> SHEET_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "sheet_blocks"));

    public static final TagKey<Item> CAKE_FLOWERS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "cake_flowers"));

    private ModTags() {
    }
}
