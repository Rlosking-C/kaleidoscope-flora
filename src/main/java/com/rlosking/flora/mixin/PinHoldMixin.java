package com.rlosking.flora.mixin;

import com.rlosking.flora.init.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PinHoldMixin {

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void flora$holdPinned(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() || !(self instanceof Mob mob)) {
            return;
        }
        if (!mob.hasEffect(ModEffects.PINNED)) {
            return;
        }
        mob.setDeltaMovement(Vec3.ZERO);
        ci.cancel();
    }
}
