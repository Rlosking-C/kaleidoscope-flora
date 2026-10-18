package com.rlosking.flora.compat;

import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (hasGuideFlag(serverPlayer) || hasGuide(serverPlayer)) {
            markGuideGiven(serverPlayer);
            return;
        }
        markGuideGiven(serverPlayer);
        ItemStack book = bookStack();
        if (!book.isEmpty()) {
            serverPlayer.getInventory().add(book);
        }
    }

    private static final ResourceLocation GIVEN_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "flora_guide_given");

    private static boolean hasGuideFlag(ServerPlayer player) {
        var holder = player.server.getAdvancements().get(GIVEN_ADVANCEMENT);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void markGuideGiven(ServerPlayer player) {
        var holder = player.server.getAdvancements().get(GIVEN_ADVANCEMENT);
        if (holder != null) {
            player.getAdvancements().award(holder, "given");
        }
    }

    private static boolean hasGuide(ServerPlayer player) {
        Item guide = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(ResourceLocation.fromNamespaceAndPath(PATCHOULI, "guide_book"));
        if (guide == null) {
            return false;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(guide)) {
                return true;
            }
        }
        return player.getOffhandItem().is(guide);
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
