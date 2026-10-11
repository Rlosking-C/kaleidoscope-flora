package com.rlosking.flora.effect;

import com.rlosking.flora.FloraConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;

public class FadeawayEffect extends MobEffect {
    public FadeawayEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD8D8C8);
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
        double range = 24.0 * FloraConfig.auraRangeMultiplier();
        for (Monster monster : entity.level().getEntitiesOfClass(Monster.class,
                entity.getBoundingBox().inflate(range))) {
            if (monster.getTarget() == entity) {
                monster.setTarget(null);
            }
        }
        for (Phantom phantom : entity.level().getEntitiesOfClass(Phantom.class,
                entity.getBoundingBox().inflate(range))) {
            if (phantom.getTarget() == entity) {
                phantom.setTarget(null);
            }
        }
        return true;
    }
}
