package com.rlosking.flora.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rlosking.flora.ModEffects;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Air, told it is water (3 of 4). {@code Player#travel} adds an upward shove
 * along the look vector while swimming - but only when the block the player
 * is about to enter is filled with fluid. In mid-air that test reads "empty",
 * so looking up would do nothing at all; while drifting it answers "not
 * empty" and the stroke works exactly as it does in water.
 *
 * <p>Approach borrowed from Kaleidoscope End's 梦境 effect.</p>
 */
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
