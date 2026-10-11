package com.rlosking.flora.compat;

import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class PatchouliGuide {

    public static final String PATCHOULI = "patchouli";

    public static final ResourceLocation BOOK =
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "flora_guide");

    private static final String GIVEN = "flora_guide_given";
    private static final String SHOWN = "flora_guide_shown";

    private PatchouliGuide() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(PATCHOULI);
    }

    public static ItemStack bookStack() {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }
        return PatchouliBook.stack(BOOK);
    }

    public static void giveOnce(Player player) {
        if (!isLoaded()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(GIVEN)) {
            return;
        }
        data.putBoolean(GIVEN, true);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack book = bookStack();
        if (!book.isEmpty() && !serverPlayer.getInventory().contains(book)) {
            serverPlayer.getInventory().add(book);
        }
    }

    public static void tickFlowerUnlock(ServerPlayer player) {
        if (!isLoaded() || player.tickCount % 20 != 0) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        CompoundTag shown = data.getCompound(SHOWN);
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!KaleidoscopeFlora.MOD_ID.equals(id.getNamespace()) || !id.getPath().endsWith("_tea_bag")) {
                continue;
            }
            String flower = id.getPath().substring(0, id.getPath().length() - "_tea_bag".length());
            if (shown.getBoolean(flower)) {
                continue;
            }
            shown.putBoolean(flower, true);
            if (shown.getAllKeys().size() == 1) {
                giveOnce(player);
            }
            data.put(SHOWN, shown);
            ResourceLocation entry = ResourceLocation.fromNamespaceAndPath(
                    KaleidoscopeFlora.MOD_ID, "flowers/" + flower);
            PatchouliBook.openEntry(player, BOOK, entry, 0);
            return;
        }
    }

    public static void applyConfigFlags() {
        if (!isLoaded() || !ModList.get().isLoaded("vanillabackport")) {
            return;
        }
        vazkii.patchouli.api.PatchouliAPI.get().setConfigFlag("kaleidoscope_flora:vanillabackport", true);
    }
}
