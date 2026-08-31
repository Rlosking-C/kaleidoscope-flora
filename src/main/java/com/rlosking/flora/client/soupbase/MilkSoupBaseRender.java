package com.rlosking.flora.client.soupbase;

import com.github.ysbbbbbb.kaleidoscopecookery.api.client.render.ISoupBaseRender;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Renders the milk surface inside a stockpot. Cooking/finished stages use the
 * visuals texture supplied by the recipe (see {@code hanami_tale.json}); the
 * ingredient stage uses our own milk animation strip from the block atlas.
 */
public class MilkSoupBaseRender implements ISoupBaseRender {
    private static final ResourceLocation MILK_SURFACE =
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "stockpot/milk_cooking");

    /**
     * Ingredient stage: no recipe is active yet, so the surface is our own
     * milk animation strip pulled from the block atlas.
     */
    @Override
    public void renderWhenPutIngredient(StockpotBlockEntity pot, float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight, int packedOverlay, float height) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(MILK_SURFACE);
        ISoupBaseRender.renderSurface(sprite, 0xFFFFFFFF, poseStack, buffer, packedLight, height);
    }

    /** Cooking stage: the recipe's own visuals texture (milk-pink waves). */
    @Override
    public void renderWhenCooking(StockpotBlockEntity pot, float partialTick, PoseStack poseStack,
                                   MultiBufferSource buffer, int packedLight, int packedOverlay,
                                   ResourceLocation cookingTexture, float height) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(cookingTexture);
        ISoupBaseRender.renderSurface(sprite, 0xFFFFFFFF, poseStack, buffer, packedLight, height);
    }

    /** Finished stage: the recipe's own visuals texture (milk-pink soup). */
    @Override
    public void renderWhenFinished(StockpotBlockEntity pot, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay,
                                    ResourceLocation finishedTexture, float height) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(finishedTexture);
        ISoupBaseRender.renderSurface(sprite, 0xFFFFFFFF, poseStack, buffer, packedLight, height);
    }
}
