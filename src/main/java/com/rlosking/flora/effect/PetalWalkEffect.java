package com.rlosking.flora.effect;

import com.rlosking.flora.event.FloraEvents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PetalWalkEffect extends MobEffect {
    private static final Map<UUID, Long> SPLASH_CLOCK = new HashMap<>();

    public static void clearPlayer(UUID uuid) {
        SPLASH_CLOCK.remove(uuid);
    }

    public static final double PLUNGE_SPEED = 0.55;

    public PetalWalkEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xEBA2C8);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return true;
        }
        BlockPos under = BlockPos.containing(entity.getX(), entity.getY() - 0.15, entity.getZ());
        if (!entity.onGround() || !level.getFluidState(under).is(FluidTags.WATER)) {
            return true;
        }
        ServerLevel server = (ServerLevel) level;
        double strideDist = entity instanceof Player walker
                ? Math.hypot(FloraEvents.lastMove(walker)[0], FloraEvents.lastMove(walker)[1])
                : entity.getDeltaMovement().horizontalDistance();
        boolean striding = strideDist > 0.01;
        if (striding) {
            if (entity.getRandom().nextInt(2) == 0) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        entity.getX(), entity.getY() + 0.1, entity.getZ(), 2, 0.3, 0.02, 0.3, 0.01);
            }
            server.sendParticles(ParticleTypes.SPLASH,
                    entity.getX(), entity.getY() + 0.1, entity.getZ(), 8, 0.4, 0.1, 0.4, 0.05);
            server.sendParticles(ParticleTypes.FALLING_WATER,
                    entity.getX(), entity.getY() + 0.4, entity.getZ(), 2, 0.3, 0.05, 0.3, 0.0);
            Long lastSplash = SPLASH_CLOCK.get(entity.getUUID());
            if (lastSplash == null || System.currentTimeMillis() - lastSplash >= 250L) {
                SPLASH_CLOCK.put(entity.getUUID(), System.currentTimeMillis());
                level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_SPLASH,
                        SoundSource.PLAYERS, 0.12f, 1.0f + entity.getRandom().nextFloat() * 0.3f);
            }
        } else if (entity.getRandom().nextInt(4) == 0) {
            server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    entity.getX(), entity.getY() + 0.1, entity.getZ(), 2, 0.3, 0.02, 0.3, 0.01);
        }
        return true;
    }
}
