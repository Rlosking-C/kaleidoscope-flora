package com.rlosking.flora.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class PatchouliBook {

    private PatchouliBook() {
    }

    static ItemStack stack(ResourceLocation book) {
        return vazkii.patchouli.api.PatchouliAPI.get().getBookStack(book);
    }

    static void openEntry(ServerPlayer player, ResourceLocation book, ResourceLocation entry, int page) {
        vazkii.patchouli.api.PatchouliAPI.get().openBookEntry(player, book, entry, page);
    }
}
