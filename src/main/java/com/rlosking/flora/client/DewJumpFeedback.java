package com.rlosking.flora.client;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Dew Flower Cake's mid-air jump: a ring of petals at the feet and one quiet
 * sound.
 *
 * <p><b>Straight from the design (section 1.6).</b> It specifies a ring of
 * petals released horizontally at the feet, roughly 0.8 blocks across with a
 * slight downward drift, as the jump's "visual anchor" - plus a single soft
 * sound, "at low volume, so that jumping repeatedly does not become tiring to
 * hear". Neither half had been implemented; testing asked for the particles.</p>
 *
 * <p><b>Client-side, and that is the right side.</b> The jump itself is
 * simulated on the client (player movement is client-authoritative), so the
 * feedback belongs there too: it lands on the same tick with no round trip, and
 * nothing about it needs the server's agreement.</p>
 *
 * <p><b>Why this lives in its own class rather than inside the mixin.</b> A
 * mixin class is not an ordinary class - its members are subject to Mixin's own
 * rules about what it may declare, and a helper that is only ever called from one
 * injection point has no reason to be part of the transformed class. Keeping the
 * mixin down to the field and the call also keeps the injection readable.</p>
 *
 * <p><b>A falling particle is correct here.</b> Elsewhere in this mod, a ring
 * that has to <em>persist</em> needed a particle which hovers - a falling one
 * turned the circle into vertical streaks. This is a one-off burst at the moment
 * of the jump, and the design explicitly asks for downward drift, so the petal
 * particle (the same {@code CHERRY_LEAVES} {@code PetalWalkEffect} uses, and one
 * of the three vanilla particles the design sanctions) is exactly right.</p>
 */
public final class DewJumpFeedback {

    /** Petals in the ring. */
    private static final int PETALS = 12;

    /** Ring radius in blocks - the design says "about 0.8". */
    private static final double RADIUS = 0.8;

    /** The design's "slight downward drift": a petal should settle, not hang. */
    private static final double SINK = -0.05;

    /** Height above the feet to spawn at, so the ring is not inside the floor. */
    private static final double LIFT = 0.1;

    /** Deliberately low: the design warns against listening fatigue. */
    private static final float VOLUME = 0.35F;

    /**
     * Upward velocity above which a mid-air jump counts as "still rising", i.e.
     * mashed rather than deliberate.
     *
     * <p><b>0.20 is "pressed in the top part of the first jump".</b> The impulse
     * is 0.42 and gravity eats roughly 0.08 per tick, so the per-tick velocities
     * of a plain jump run 0.42, 0.33, 0.25, 0.17, 0.08, 0.00, -0.08. A player who
     * hammers the key spends their jump on the first tick the mixin allows - the
     * second airborne tick, at about 0.25 - whereas a player who waits for the
     * rise to finish presses at 0.08 or below. 0.20 sits between them and, being
     * a third of the impulse, reads as a rule rather than a magic number: <b>a
     * deliberate second jump is one taken in the top two thirds of the first
     * jump's climb.</b></p>
     *
     * <p>The first version used 0.08, which demanded near-perfect apex timing and
     * refused a press one tick early. Reported as "sometimes the double jump does
     * not trigger the landing damage".</p>
     *
     * <p><b>Why this has to be measured rather than inferred.</b>
     * {@code LivingEntity#jumpFromGround} sets the vertical velocity
     * <em>unconditionally</em>, so an early press does not give a smaller boost -
     * it gives a full one from wherever the player had reached. That puts a
     * mashed hop around 2.0 blocks and a deliberate double jump around 2.5,
     * against a plain single jump's 1.25: the two cases sit on opposite sides of
     * the design's own two-block floor and their ranges overlap it.</p>
     */
    public static final double RISING_VELOCITY = 0.20;

    /** Plays the ring and the sound at the player's feet. */
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
        // playLocalSound rather than playSound: this is feedback for the player
        // who jumped, and it must not be broadcast as a world sound event.
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, VOLUME, 1.0F, false);
    }

    private DewJumpFeedback() {
    }
}
