package com.rlosking.flora.mixin;

import com.rlosking.flora.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class ToastedHungerMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float flora$doubleExhaustion(float exhaustion) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || !self.hasEffect(ModEffects.TOASTED_CAKE)) {
            return exhaustion;
        }
        return exhaustion * 2.0f;
    }
}
