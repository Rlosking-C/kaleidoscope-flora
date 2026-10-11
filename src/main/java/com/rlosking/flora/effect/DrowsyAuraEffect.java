package com.rlosking.flora.effect;

import com.rlosking.flora.FloraConfig;
import com.rlosking.flora.init.ModEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class DrowsyAuraEffect extends MobEffect {

    private static final int CUP_SECONDS = 180;

    private static final Map<UUID, long[]> LAST_CUP = new HashMap<>();

    private static boolean stacking;

    public DrowsyAuraEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9C8FB8);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        Level level = entity.level();
        double range = 7.0 * FloraConfig.auraRangeMultiplier();
        for (Monster monster : level.getEntitiesOfClass(Monster.class,
                entity.getBoundingBox().inflate(range))) {
            monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }
        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class,
                entity.getBoundingBox().inflate(range))) {
            if (phantom.getTarget() == entity) {
                phantom.setTarget(null);
            }
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || stacking) {
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        MobEffectInstance active = player.getEffect(ModEffects.DROWSY);
        if (active == null) {
            return;
        }
        long now = entity.level().getGameTime();
        long[] last = LAST_CUP.get(player.getUUID());
        if (last == null) {
            LAST_CUP.put(player.getUUID(), new long[] {now, active.getDuration()});
            return;
        }
        long remaining = Math.max(0L, last[1] - (now - last[0]));
        if (remaining == 0) {
            LAST_CUP.put(player.getUUID(), new long[] {now, active.getDuration()});
            return;
        }
        int desired = (int) Math.min(remaining + (long) CUP_SECONDS * 20, Integer.MAX_VALUE);
        stacking = true;
        try {
            player.addEffect(new MobEffectInstance(ModEffects.DROWSY, desired,
                    active.getAmplifier()));
        } finally {
            stacking = false;
        }
        LAST_CUP.put(player.getUUID(), new long[] {now, desired});
    }
}
