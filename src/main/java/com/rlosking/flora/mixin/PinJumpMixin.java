package com.rlosking.flora.mixin;

import com.rlosking.flora.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class PinJumpMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void flora$forbidJumpWhilePinned(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModEffects.PINNED)) {
            self.setJumping(false);
        }
    }
}
