package com.rlosking.flora.compat;

import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Patchouli bridge for the in-game guide (v0.3.5).
 *
 * <p><b>Soft dependency.</b> Patchouli is declared {@code optional} in
 * neoforge.mods.toml and is only a {@code compileOnly} build dependency, so the
 * mod must behave identically without it. Every method below returns early via
 * {@link #isLoaded()} before it touches a Patchouli type, and this class is the
 * the two calls that actually touch Patchouli live in {@link PatchouliBook},
 * which is only reachable once this class' {@link #isLoaded()} returned true.
 * That keeps the JVM from ever resolving {@code vazkii.patchouli.*} for a player
 * who does not have the mod.</p>
 *
 * <p><b>Why the ticking hook exists.</b> The design wants the guide to flip to a
 * flower's page the first time the player picks that flower up. Patchouli can
 * gate entries behind advancements, but "auto-open on pickup" is not something
 * the data files can express, so the inventory is polled once a second here and
 * the entry is opened through {@code PatchouliAPI} when a new tea bag appears.</p>
 */
public final class PatchouliGuide {

    /** Patchouli's mod id, as used by the optional dependency declaration. */
    public static final String PATCHOULI = "patchouli";

    /** The book id, i.e. data/kaleidoscope_flora/patchouli_books/flora_guide/. */
    public static final ResourceLocation BOOK =
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "flora_guide");

    /** Player persistent-data keys: the guide was handed out / a flower page was shown. */
    private static final String GIVEN = "flora_guide_given";
    private static final String SHOWN = "flora_guide_shown";

    private PatchouliGuide() {
    }

    /** True when Patchouli is installed. Safe to call unconditionally. */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(PATCHOULI);
    }

    /**
     * The guide as an item stack, or an empty stack when Patchouli is absent.
     * Patchouli hands out one generic guide item carrying a book component
     * rather than registering an item per book. The call itself lives in
     * {@link PatchouliBook} so that this class stays free of Patchouli types.
     */
    public static ItemStack bookStack() {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }
        return PatchouliBook.stack(BOOK);
    }

    /** Hands the guide to the player once, on their first join. */
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

    /**
     * Once per second: if the player carries a tea bag whose page has not been
     * shown yet, flip the guide to that page. Only one page per call, so a
     * player who picks up the whole flower set is not buried in GUIs.
     *
     * <p>Entry ids are derived from the item id: {@code <flower>_tea_bag}
     * becomes {@code kaleidoscope_flora:flowers/<flower>}. That is why the
     * entries live in {@code entries/flowers/} regardless of their category.</p>
     */
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
                // First flower ever: make sure the guide is actually in hand.
                giveOnce(player);
            }
            data.put(SHOWN, shown);
            ResourceLocation entry = ResourceLocation.fromNamespaceAndPath(
                    KaleidoscopeFlora.MOD_ID, "flowers/" + flower);
            PatchouliBook.openEntry(player, BOOK, entry, 0);
            return;
        }
    }
}
