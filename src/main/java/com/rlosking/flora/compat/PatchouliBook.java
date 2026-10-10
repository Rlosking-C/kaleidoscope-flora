package com.rlosking.flora.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The only class in this mod that names a {@code vazkii.patchouli} type.
 *
 * <p><b>Why it is separate.</b> {@link PatchouliGuide} is loaded on every launch,
 * with or without Patchouli, so nothing in it may reference a Patchouli type -
 * otherwise class verification could try to resolve {@code vazkii.patchouli.*}
 * on a player who does not have the mod. This class is touched only after
 * {@link PatchouliGuide#isLoaded()} returned true, which means the JVM never has
 * to load it otherwise.</p>
 */
final class PatchouliBook {

    private PatchouliBook() {
    }

    /** The guide item carrying the book component, or an empty stack. */
    static ItemStack stack(ResourceLocation book) {
        return vazkii.patchouli.api.PatchouliAPI.get().getBookStack(book);
    }

    /** Opens (or flips) the guide at one entry, server side. */
    static void openEntry(ServerPlayer player, ResourceLocation book, ResourceLocation entry, int page) {
        vazkii.patchouli.api.PatchouliAPI.get().openBookEntry(player, book, entry, page);
    }
}
