package com.rlosking.flora.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class FloatingEffect extends MobEffect {
    public FloatingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xBBD4F5);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        entity.resetFallDistance();
        return true;
    }
}
