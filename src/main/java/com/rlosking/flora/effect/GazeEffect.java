package com.rlosking.flora.effect;

import com.rlosking.flora.FloraConfig;
import com.rlosking.flora.init.FloraAdvancements;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;

public class GazeEffect extends MobEffect {

    private static final int FREEZE_TICKS = 80;

    private static final long STALE_GAP_TICKS = 5;

    private static final Map<UUID, Map<UUID, Integer>> STARE = new HashMap<>();

    private static final Map<UUID, Long> STARE_CLOCK = new HashMap<>();

    public static void clearPlayer(UUID uuid) {
        STARE.remove(uuid);
        STARE_CLOCK.remove(uuid);
    }

    public GazeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9FB6D9);
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
        ServerLevel server = (ServerLevel) entity.level();
        UUID drinker = entity.getUUID();
        long now = server.getGameTime();

        Map<UUID, Integer> stares = STARE.computeIfAbsent(drinker, k -> new HashMap<>());
        Long lastTick = STARE_CLOCK.get(drinker);
        if (lastTick == null || now - lastTick > STALE_GAP_TICKS) {
            stares.clear();
        }
        STARE_CLOCK.put(drinker, now);

        Set<UUID> stared = new HashSet<>();
        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getViewVector(1.0F);
        double gazeRange = 25.0 * FloraConfig.auraRangeMultiplier();
        for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(gazeRange))) {
            if (other == entity) {
                continue;
            }
            if (now % 10 == 0) {
                other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false));
            }
            Vec3 toOther = other.getEyePosition().subtract(eye);
            double distance = toOther.length();
            boolean seen = distance < 0.5
                    || (look.dot(toOther.normalize()) >= 0.966 && entity.hasLineOfSight(other));
            if (!seen) {
                continue;
            }
            stared.add(other.getUUID());
            if (other instanceof EnderMan && entity instanceof ServerPlayer starer) {
                FloraAdvancements.award(starer, FloraAdvancements.EVENT_GAZE_ABYSS);
            }
            int ticks = stares.getOrDefault(other.getUUID(), 0) + 1;
            stares.put(other.getUUID(), ticks);
            if (ticks < FREEZE_TICKS) {
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, true, true));
            } else {
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
            }
        }
        stares.keySet().retainAll(stared);
        return true;
    }
}
