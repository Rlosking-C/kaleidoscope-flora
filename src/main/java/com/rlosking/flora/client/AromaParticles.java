package com.rlosking.flora.client;

import com.rlosking.flora.FloraParticleTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Vector3f;

/**
 * Falling bloom petal. Three clearly-readable petal silhouettes (16x16,
 * textures/particle/aroma_0..2.png) tinted per flower colour via
 * DustParticleOptions. Each petal drifts DOWN with a terminal fall speed,
 * sways side to side and tumbles, then disappears the moment it lands
 * (v0.4.0: no ground rest). Opaque, never alpha-faded: a
 * petal is a solid object, not smoke.
 */
public final class AromaParticles {

    public static void register(IEventBus modBus) {
        modBus.addListener(AromaParticles::onRegisterProviders);
    }

    private static void onRegisterProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FloraParticleTypes.AROMA.get(), AromaPetal.Provider::new);
    }

    static final class AromaPetal extends TextureSheetParticle {
        /** Hard cap: even a petal that never lands is gone after 15 s.
         *  v0.4.0 spec: single-petal lifetime is 300 ticks; the steady-state
         *  count is density x lifetime, so this number is a performance knob. */
        private static final int MAX_LIFETIME = 300;
        /** Fall acceleration (blocks/tick^2) - unmistakably falling. */
        private static final float FALL_ACCELERATION = 0.035F;
        /** Terminal descent speed: a slow, leaf-like flutter (~1.1 b/s). */
        private static final float MAX_FALL_SPEED = 0.055F;
        // v0.4.0 spec: NO ground rest. A petal disappears the moment it lands -
        // the rest was neither vanilla behaviour nor free: it extended every
        // petal's life and so raised the steady-state particle count.

        private final float swayPhase;
        private final float swaySpeed;
        private final float swayAmplitude;
        private final float windX;
        private final float windZ;
        private float rollSpeed;

        private AromaPetal(ClientLevel level, SpriteSet sprites, double x, double y, double z,
                           double xSpeed, double zSpeed, DustParticleOptions options) {
            super(level, x, y, z);
            this.pickSprite(sprites);
            this.lifetime = MAX_LIFETIME;
            this.gravity = 0.0F;       // falling is handled manually below
            this.friction = 1.0F;
            this.hasPhysics = true;    // move() collides with blocks, sets onGround
            this.setSize(0.1F, 0.1F);  // small collision box; the sprite is bigger
            this.swayPhase = this.random.nextFloat() * Mth.TWO_PI;
            this.swaySpeed = 0.05F + this.random.nextFloat() * 0.05F;
            this.swayAmplitude = 0.02F + this.random.nextFloat() * 0.02F;
            this.rollSpeed = (float) Math.toRadians(this.random.nextBoolean() ? -1.0 : 1.0)
                    * (2.0F + this.random.nextFloat() * 5.0F);  // ~40..140 deg/s
            this.windX = (float) xSpeed;
            this.windZ = (float) zSpeed;
            // v0.4.0 spec: quadSize is 0.05, not a performance knob - the
            // coverage target (Phi ~ 0.8 x the tavern baseline) is met by the
            // density and the range instead. Anything above 0.12 reads as a
            // colour block rather than a petal.
            this.quadSize = 0.05F * Mth.clamp(options.getScale(), 0.25F, 3.0F);
            Vector3f color = options.getColor();
            this.setColor(color.x, color.y, color.z);
        }

        @Override
        public ParticleRenderType getRenderType() {
            // Opaque, like vanilla CherryParticle - translucency made the
            // sprite blur into haze that reads as smoke.
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.oRoll = this.roll;
            this.age++;
            if (this.lifetime-- <= 0) {
                this.remove();
                return;
            }
            // Falling: accelerate to a terminal flutter speed.
            this.yd -= FALL_ACCELERATION;
            if (this.yd < -MAX_FALL_SPEED) {
                this.yd = -MAX_FALL_SPEED;
            }
            // Side-to-side sway with a per-petal phase, plus the shared breeze.
            float sway = (this.age + this.swayPhase) * this.swaySpeed;
            this.xd = Mth.sin(sway) * this.swayAmplitude + this.windX;
            this.zd = Mth.cos(sway * 0.83F) * this.swayAmplitude * 0.8F + this.windZ;
            // Tumble.
            this.roll += this.rollSpeed;
            this.move(this.xd, this.yd, this.zd);
            if (this.onGround) {
                // Landed: gone. Vanilla cherry petals do the same, and it keeps
                // the steady-state count at density x lifetime.
                this.remove();
            }
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<DustParticleOptions> {
            @Override
            public Particle createParticle(DustParticleOptions options, ClientLevel level,
                                           double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed) {
                return new AromaPetal(level, this.sprites, x, y, z, xSpeed, zSpeed, options);
            }
        }
    }

    private AromaParticles() {
    }
}
