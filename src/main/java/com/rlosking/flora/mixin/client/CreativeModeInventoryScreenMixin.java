package com.rlosking.flora.mixin.client;

import com.rlosking.flora.client.creativetab.FloraCreativeFilter;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
