package com.rlosking.flora.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the creative screen's currently selected tab.
 *
 * <p><b>Why an accessor rather than an event.</b> The side column has to know
 * whether this mod's tab is the one on screen - to decide whether to draw itself,
 * and whether filtering is even appropriate. That tab is a
 * <b>private static field</b> of {@code CreativeModeInventoryScreen}, and NeoForge
 * publishes no event for tab selection, so the only way to read it is through the
 * class itself.</p>
 *
 * <p>The field is static, so the accessor method must be static too; Mixin
 * requires that pairing. The body never runs - Mixin replaces the call site - but
 * an interface method needs one, and throwing keeps that honest if it is ever
 * invoked by mistake.</p>
 *
 * <p>Nothing here is copied from another mod: the technique is the standard Mixin
 * accessor pattern, and the field name was read out of the 1.21.1 class itself
 * with {@code javap}.</p>
 */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {

    /** The tab currently shown in the creative inventory. */
    @Accessor("selectedTab")
    static CreativeModeTab flora$selectedTab() {
        throw new AssertionError("replaced by Mixin");
    }
}
