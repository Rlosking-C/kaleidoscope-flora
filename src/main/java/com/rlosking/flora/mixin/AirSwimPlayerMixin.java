package com.rlosking.flora.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rlosking.flora.init.ModEffects;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class AirSwimPlayerMixin {

    @WrapOperation(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;isEmpty()Z"))
    private boolean flora$airIsNotEmpty(FluidState instance, Operation<Boolean> original) {
        return (Object) this instanceof net.minecraft.world.entity.player.Player player
                && ModEffects.isAirborne(player)
                || original.call(instance);
    }
}
