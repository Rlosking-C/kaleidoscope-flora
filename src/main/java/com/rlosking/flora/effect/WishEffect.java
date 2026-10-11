package com.rlosking.flora.effect;

import com.rlosking.flora.init.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public class WishEffect extends MobEffect {
    public WishEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
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
        int blessing = ModEffects.scaledTicks(120);
        MobEffectInstance[] pool = {
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, blessing, 1),
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, blessing, 1),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, blessing, 1),
                new MobEffectInstance(MobEffects.DIG_SPEED, blessing, 1),
                new MobEffectInstance(MobEffects.LUCK, blessing, 0)};
        entity.addEffect(pool[entity.getRandom().nextInt(pool.length)]);
        if (entity.level() instanceof ServerLevel server) {
            server.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.0F), 0.9F),
                    entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        }
        return true;
    }
}
