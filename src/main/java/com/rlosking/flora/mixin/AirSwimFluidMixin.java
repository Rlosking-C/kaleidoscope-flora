package com.rlosking.flora.mixin;

import com.rlosking.flora.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Air, told it is water (1 of 4). NeoForge models "no fluid" as the EMPTY
 * fluid type, and air is full of it; it is not swimmable, so the movement
 * code refuses to swim in it. While the mooncake's Floating effect is up,
 * the empty fluid becomes swimmable for that player.
 *
 * <p>Approach borrowed from Kaleidoscope End's 梦境 effect, the reference
 * implementation for air swimming - see also {@link AirSwimEntityMixin},
 * {@link AirSwimPlayerMixin} and the client-side twin.</p>
 */
@Mixin(FluidType.class)
public abstract class AirSwimFluidMixin {

    @Inject(method = "canSwim", at = @At("RETURN"), cancellable = true, remap = false)
    private void flora$airSwim(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Only the empty fluid: water is already swimmable and lava must
        // stay exactly as dangerous as it was.
        if (!cir.getReturnValueZ()
                && (Object) this == NeoForgeMod.EMPTY_TYPE.value()
                && entity instanceof Player player
                && ModEffects.isAirborne(player)) {
            cir.setReturnValue(true);
        }
    }
}
