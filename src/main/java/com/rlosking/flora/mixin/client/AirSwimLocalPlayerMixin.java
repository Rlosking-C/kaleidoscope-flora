package com.rlosking.flora.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rlosking.flora.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiPredicate;

/**
 * Air, told it is water (4 of 4, client side). The local player's own
 * movement is simulated on the client, so the two queries {@code aiStep}
 * asks about swimming have to answer the same way there - otherwise the
 * client keeps walking through the air while the server thinks it is
 * swimming, and the two drift apart.
 *
 * <p>Approach borrowed from Kaleidoscope End's 梦境 effect.</p>
 */
@Mixin(LocalPlayer.class)
public abstract class AirSwimLocalPlayerMixin {

    @WrapOperation(method = "aiStep()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;canStartSwimming()Z"))
    private boolean flora$canStartSwimming(LocalPlayer instance, Operation<Boolean> original) {
        return ModEffects.isAirborne(instance) || original.call(instance);
    }

    @WrapOperation(method = "aiStep()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isInFluidType(Ljava/util/function/BiPredicate;)Z"))
    private boolean flora$isInFluidType(LocalPlayer instance, BiPredicate<FluidType, Double> predicate,
                                        Operation<Boolean> original) {
        return ModEffects.isAirborne(instance) || original.call(instance, predicate);
    }
}
