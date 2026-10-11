package com.rlosking.flora.effect;

import com.rlosking.flora.init.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class PinnedEffect extends MobEffect {

    public PinnedEffect() {
        super(MobEffectCategory.HARMFUL, 0xC8E4F0);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel server)) {
            return true;
        }
        MobEffectInstance self = entity.getEffect(ModEffects.PINNED);
        double radius = ModEffects.pinRingRadius(entity);
        int count = ModEffects.pinRingCount(radius);
        double lift = ModEffects.pinRingLift(entity);
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2.0;
            server.sendParticles(ParticleTypes.END_ROD,
                    entity.getX() + Math.cos(angle) * radius,
                    entity.getY() + lift,
                    entity.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }
}
