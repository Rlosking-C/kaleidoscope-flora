package com.rlosking.flora.effect;

import com.rlosking.flora.FloraConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class WitherAuraEffect extends MobEffect {
    public WitherAuraEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3A3A3A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        ServerLevel server = (ServerLevel) entity.level();
        double range = 7.0 * FloraConfig.auraRangeMultiplier();
        for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(range))) {
            if (other == entity) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            server.sendParticles(ParticleTypes.SMOKE,
                    other.getX(), other.getY() + other.getBbHeight() * 0.5, other.getZ(),
                    3, 0.15, 0.25, 0.15, 0.01);
        }
        return true;
    }
}
