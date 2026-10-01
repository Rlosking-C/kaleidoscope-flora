package com.rlosking.flora.mixin.client;

import com.rlosking.flora.client.creativetab.FloraCreativeFilter;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-applies the section filter whenever the creative screen builds its item
 * list.
 *
 * <p><b>Why a mixin at all.</b> Selecting a tab is what fills the item list, and
 * NeoForge publishes no event for it - the screen does it internally. So the one
 * thing an event cannot cover is "the player just switched to our tab", and that
 * is exactly when the section has to be honoured. Everything else about the
 * column (drawing, clicks, tooltips) is done with ordinary widgets and needs no
 * injection.</p>
 *
 * <p><b>Both entry points, because either can be the one that fills the list.</b>
 * {@code selectTab} is what runs when a tab button is pressed; {@code init} is
 * what runs when the screen is first opened. Injecting at the tail of both means
 * the list is filtered whichever path built it, and applying it twice is
 * harmless - {@link FloraCreativeFilter#apply} always rebuilds from the tab's own
 * catalogue, so it is idempotent and cannot compound.</p>
 *
 * <p>Both methods are private, which makes their names unambiguous for Mixin's
 * method resolution - there is no overload to disambiguate.</p>
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void flora$filterOnTabSelected(CreativeModeTab tab, CallbackInfo ci) {
        FloraCreativeFilter.apply((CreativeModeInventoryScreen) (Object) this);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void flora$filterOnInit(CallbackInfo ci) {
        FloraCreativeFilter.apply((CreativeModeInventoryScreen) (Object) this);
    }
}
