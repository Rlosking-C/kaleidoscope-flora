package com.rlosking.flora.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rlosking.flora.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiPredicate;

/**
 * Air, told it is water (2 of 4). The three fluid queries the engine asks
 * while deciding how an entity moves are answered "yes, you are submerged"
 * for a floating player in mid-air, so {@code LivingEntity#travel} takes its
 * ordinary water branch and the entity never enters its falling branch.
 *
 * <p>Approach borrowed from Kaleidoscope End's 梦境 effect.</p>
 */
@Mixin(Entity.class)
public abstract class AirSwimEntityMixin {

    /**
     * The height of a fluid the entity is standing in. Reporting the full
     * block for the EMPTY fluid is what makes everything downstream - the
     * "touching water" flag, the eye-in-fluid test, {@code travel}'s water
     * branch - agree that this is water.
     */
    @Inject(method = "getFluidTypeHeight", at = @At("RETURN"), cancellable = true, remap = false)
    private void flora$airHeight(FluidType type, CallbackInfoReturnable<Double> cir) {
        if (type == NeoForgeMod.EMPTY_TYPE.value()
                && cir.getReturnValueD() <= 0.0D
                && (Object) this instanceof Player player
                && ModEffects.isAirborne(player)) {
            cir.setReturnValue(1.0D);
        }
    }

    /** Without this the swim state never starts, so no stroke and no swim pose. */
    @WrapOperation(method = "updateSwimming()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;canStartSwimming()Z"))
    private boolean flora$canStartSwimming(Entity instance, Operation<Boolean> original) {
        return instance instanceof Player player && ModEffects.isAirborne(player)
                || original.call(instance);
    }

    /** Asked both by {@code updateSwimming} and by the crawl pose test. */
    @WrapOperation(method = {"updateSwimming()V", "isVisuallyCrawling()Z"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;isInFluidType(Ljava/util/function/BiPredicate;)Z"))
    private boolean flora$isInFluidType(Entity instance, BiPredicate<FluidType, Double> predicate,
                                        Operation<Boolean> original) {
        return instance instanceof Player player && ModEffects.isAirborne(player)
                || original.call(instance, predicate);
    }

    /** Plain "am I in any fluid at all" - the empty fluid counts while drifting. */
    @Inject(method = "isInFluidType()Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void flora$isInAnyFluid(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && (Object) this instanceof Player player
                && ModEffects.isAirborne(player)) {
            cir.setReturnValue(true);
        }
    }
}
