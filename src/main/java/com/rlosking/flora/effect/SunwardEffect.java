package com.rlosking.flora.effect;

import com.rlosking.flora.init.FloraAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class SunwardEffect extends MobEffect {
    public SunwardEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD835);
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
        Level level = entity.level();
        if (level.isDay() && level.canSeeSky(entity.blockPosition())) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
            if (entity instanceof ServerPlayer player) {
                FloraAdvancements.sunwardDayForm(player);
            }
        } else if (!level.isDay()) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
        }
        return true;
    }
}
