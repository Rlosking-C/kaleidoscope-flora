package com.rlosking.flora.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class DewJumpFeedback {

    private static final int PETALS = 12;

    private static final double RADIUS = 0.8;

    private static final double SINK = -0.05;

    private static final double LIFT = 0.1;

    private static final float VOLUME = 0.35F;

    public static final double RISING_VELOCITY = 0.20;

    public static void play(Player player) {
        Level level = player.level();
        for (int i = 0; i < PETALS; i++) {
            double angle = (i / (double) PETALS) * Math.PI * 2.0;
            level.addParticle(ParticleTypes.CHERRY_LEAVES,
                    player.getX() + Math.cos(angle) * RADIUS,
                    player.getY() + LIFT,
                    player.getZ() + Math.sin(angle) * RADIUS,
                    0.0, SINK, 0.0);
        }
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, VOLUME, 1.0F, false);
    }

    private DewJumpFeedback() {
    }
}
