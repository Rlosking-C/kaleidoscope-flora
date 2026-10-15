package com.rlosking.flora.effect;

import com.rlosking.flora.FloraConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;

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
            if (other == entity || !isValidTarget(other)) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            server.sendParticles(ParticleTypes.SMOKE,
                    other.getX(), other.getY() + other.getBbHeight() * 0.5, other.getZ(),
                    3, 0.15, 0.25, 0.15, 0.01);
        }
        return true;
    }
    private static boolean isValidTarget(LivingEntity other) {
        if (other instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        if (other instanceof Enemy) {
            return true;
        }
        return other instanceof Mob mob && mob.getTarget() != null;
    }

}
