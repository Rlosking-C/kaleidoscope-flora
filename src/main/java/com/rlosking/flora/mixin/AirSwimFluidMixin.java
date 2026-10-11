package com.rlosking.flora.mixin;

import com.rlosking.flora.init.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidType.class)
public abstract class AirSwimFluidMixin {

    @Inject(method = "canSwim", at = @At("RETURN"), cancellable = true, remap = false)
    private void flora$airSwim(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && (Object) this == NeoForgeMod.EMPTY_TYPE.value()
                && entity instanceof Player player
                && ModEffects.isAirborne(player)) {
            cir.setReturnValue(true);
        }
    }
}
