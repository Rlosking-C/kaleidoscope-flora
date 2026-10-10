package com.rlosking.flora;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * Bloom aroma motes: a custom particle type that reuses {@link
 * DustParticleOptions} as its carrier, so every spawn carries the flower's
 * colour and a size factor while the client renders our own drawable
 * texture (assets/kaleidoscope_flora/textures/particle/aroma.png).
 */
public final class FloraParticleTypes {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KaleidoscopeFlora.MOD_ID);

    public static final Supplier<ParticleType<DustParticleOptions>> AROMA = PARTICLES.register("aroma",
            () -> new ParticleType<DustParticleOptions>(false) {
                @Override
                public MapCodec<DustParticleOptions> codec() {
                    return DustParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, DustParticleOptions> streamCodec() {
                    return DustParticleOptions.STREAM_CODEC;
                }
            });

    /** Options for one tinted mote; the sprite is applied on the client. */
    public static DustParticleOptions aroma(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255.0F,
                ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F), scale);
    }

    private FloraParticleTypes() {
    }
}
