package com.rlosking.flora.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DivinationEffect extends MobEffect {
    public DivinationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xEFE9C0);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 160 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        switch (entity.getRandom().nextInt(4)) {
            case 0 -> entity.heal(2.0f);
            case 1 -> {
                if (entity instanceof Player player) {
                    player.getFoodData().eat(1, 0.4f);
                }
            }
            case 2 -> entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
            default -> {
            }
        }
        return true;
    }
}
