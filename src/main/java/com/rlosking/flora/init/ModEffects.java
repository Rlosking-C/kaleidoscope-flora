package com.rlosking.flora.init;

import com.rlosking.flora.FloraConfig;
import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.effect.DivinationEffect;
import com.rlosking.flora.effect.DrowsyAuraEffect;
import com.rlosking.flora.effect.FadeawayEffect;
import com.rlosking.flora.effect.FloatingEffect;
import com.rlosking.flora.effect.FlowerPathEffect;
import com.rlosking.flora.effect.GazeEffect;
import com.rlosking.flora.effect.MarkerEffect;
import com.rlosking.flora.effect.PetalWalkEffect;
import com.rlosking.flora.effect.PinnedEffect;
import com.rlosking.flora.effect.PurgeEffect;
import com.rlosking.flora.effect.SproutEffect;
import com.rlosking.flora.effect.SunwardEffect;
import com.rlosking.flora.effect.TastebloomEffect;
import com.rlosking.flora.effect.WishEffect;
import com.rlosking.flora.effect.WitherAuraEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KaleidoscopeFlora.MOD_ID);

    public static int scaledTicks(int seconds) {
        double multiplier = FloraConfig.SPEC.isLoaded()
                ? FloraConfig.effectDurationMultiplier()
                : 1.0;
        return Math.max(1, (int) (seconds * 20 * multiplier));
    }

    public static final DeferredHolder<MobEffect, PurgeEffect> PURGE =
            EFFECTS.register("purge", PurgeEffect::new);

    public static final DeferredHolder<MobEffect, FadeawayEffect> FADEAWAY =
            EFFECTS.register("fadeaway", FadeawayEffect::new);

    public static final DeferredHolder<MobEffect, DrowsyAuraEffect> DROWSY =
            EFFECTS.register("drowsy", DrowsyAuraEffect::new);

    public static final DeferredHolder<MobEffect, PetalWalkEffect> PETALWALK =
            EFFECTS.register("petalwalk", PetalWalkEffect::new);

    public static final DeferredHolder<MobEffect, DivinationEffect> DIVINATION =
            EFFECTS.register("divination", DivinationEffect::new);

    public static final DeferredHolder<MobEffect, WitherAuraEffect> WITHER_AURA =
            EFFECTS.register("wither_aura", WitherAuraEffect::new);

    public static final DeferredHolder<MobEffect, SunwardEffect> SUNWARD =
            EFFECTS.register("sunward", SunwardEffect::new);

    public static final DeferredHolder<MobEffect, SproutEffect> SPROUT =
            EFFECTS.register("sprout", SproutEffect::new);

    public static final DeferredHolder<MobEffect, GazeEffect> GAZE =
            EFFECTS.register("gaze", GazeEffect::new);

    public static final DeferredHolder<MobEffect, WishEffect> WISH =
            EFFECTS.register("wish", WishEffect::new);

    public static final DeferredHolder<MobEffect, FlowerPathEffect> FLOWER_PATH =
            EFFECTS.register("flower_path", FlowerPathEffect::new);

    public static final DeferredHolder<MobEffect, FloatingEffect> FLOATING =
            EFFECTS.register("floating", FloatingEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> TOASTED_CAKE =
            EFFECTS.register("toasted_cake", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xD9A441));

    public static final DeferredHolder<MobEffect, MobEffect> DEW_CAKE =
            EFFECTS.register("dew_cake", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x8FD8E8));

    public static final DeferredHolder<MobEffect, MobEffect> PINNED =
            EFFECTS.register("pinned", PinnedEffect::new);

    public static final int PIN_TICKS = 20;

    private static final double PIN_MIN_RADIUS = 0.45;

    private static final double PIN_RADIUS_PER_WIDTH = 0.75;

    private static final double PIN_PARTICLE_SPACING = 0.4;

    private static final int PIN_MIN_PARTICLES = 6;

    private static final int PIN_MAX_PARTICLES = 48;

    private static final double PIN_LIFT_PER_HEIGHT = 0.35;
    private static final double PIN_MIN_LIFT = 0.15;
    private static final double PIN_MAX_LIFT = 0.6;

    public static double pinRingRadius(LivingEntity entity) {
        return Math.max(PIN_MIN_RADIUS, entity.getBbWidth() * PIN_RADIUS_PER_WIDTH);
    }

    public static int pinRingCount(double radius) {
        int count = (int) Math.round(radius * 2.0 * Math.PI / PIN_PARTICLE_SPACING);
        return Math.max(PIN_MIN_PARTICLES, Math.min(PIN_MAX_PARTICLES, count));
    }

    public static double pinRingLift(LivingEntity entity) {
        return Math.max(PIN_MIN_LIFT, Math.min(PIN_MAX_LIFT, entity.getBbHeight() * PIN_LIFT_PER_HEIGHT));
    }

    public static final DeferredHolder<MobEffect, MobEffect> TASTEBLOOM =
            EFFECTS.register("tastebloom", TastebloomEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FIREBRAND =
            EFFECTS.register("firebrand", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE86A17));

    public static final DeferredHolder<MobEffect, MobEffect> VAMPIRIC =
            EFFECTS.register("vampiric", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xB02E26));

    public static final DeferredHolder<MobEffect, MobEffect> HARVEST =
            EFFECTS.register("harvest", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE8A13A));

    public static final DeferredHolder<MobEffect, MobEffect> ABSOLVE =
            EFFECTS.register("absolve", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF2F2F2));

    public static final DeferredHolder<MobEffect, MobEffect> FEATHERFALL =
            EFFECTS.register("featherfall", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x9ED9F0));

    public static final DeferredHolder<MobEffect, MobEffect> KISS =
            EFFECTS.register("kiss", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xDCEDC2));

    public static final DeferredHolder<MobEffect, MobEffect> THORNS =
            EFFECTS.register("thorns", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC62828));

    public static final DeferredHolder<MobEffect, MobEffect> DIGESTION =
            EFFECTS.register("digestion", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x7BAF6A));

    public static final DeferredHolder<MobEffect, MobEffect> PETAL_VEIL =
            EFFECTS.register("petal_veil", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF5B5D9));

    public static final DeferredHolder<MobEffect, MobEffect> SNIFFER_SOUL =
            EFFECTS.register("sniffer_soul", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC8814F));

    public static final DeferredHolder<MobEffect, MobEffect> ECHO =
            EFFECTS.register("echo", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xA59BB8));

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    public static boolean isAirborne(LivingEntity entity) {
        return !entity.onGround() && entity.hasEffect(FLOATING);
    }
}
