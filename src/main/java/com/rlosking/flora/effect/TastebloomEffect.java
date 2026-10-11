package com.rlosking.flora.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TastebloomEffect extends MobEffect {

    static final int TASTEBLOOM_HASTE_FOOD_LEVEL = 20;

    public TastebloomEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF7E3A1);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !(entity instanceof Player player)) {
            return true;
        }
        if (player.getFoodData().getFoodLevel() >= TASTEBLOOM_HASTE_FOOD_LEVEL) {
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false));
        }
        return true;
    }
}
