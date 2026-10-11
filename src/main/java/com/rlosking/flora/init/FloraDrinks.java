package com.rlosking.flora.init;

import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.fml.ModList;

import java.util.function.Supplier;
import com.rlosking.flora.effect.ModEffects;
import com.rlosking.flora.FloraConfig;
import com.rlosking.flora.KaleidoscopeFlora;

public final class FloraDrinks {
    private static final String COOKERY_ID = "kaleidoscope_cookery";

    private static final TeacupRegistry REGISTRY = new TeacupRegistry();

    private FloraDrinks() {
    }

    private static int ticks(int seconds) {
        double multiplier = FloraConfig.SPEC.isLoaded()
                ? FloraConfig.effectDurationMultiplier()
                : 1.0;
        return Math.max(1, (int) (seconds * 20 * multiplier));
    }

    public static void registerAll() {

        register("when_the_wind_rises", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.PURGE, 1, 0))
                .addEffect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, ticks(90))));

        register("lullaby", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.DROWSY, ticks(180))));

        register("first_bloom", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.TASTEBLOOM, ticks(300))));

        register("fire_waltz", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks(180)))
                .addEffect(() -> new MobEffectInstance(ModEffects.FIREBRAND, ticks(180))));

        register("the_unnoticed", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.INVISIBILITY, ticks(60)))
                .addEffect(() -> new MobEffectInstance(ModEffects.FADEAWAY, 1, 0)));

        register("crimson_heartbeat", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.VAMPIRIC, ticks(180))));

        register("autumn_serenade", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.HARVEST, ticks(180))));

        register("absolution", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.ABSOLVE, ticks(180))));

        register("rosy_stride", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.PETALWALK, ticks(90))));

        register("loves_me_not", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.REGENERATION, ticks(90)))
                .addEffect(() -> new MobEffectInstance(ModEffects.DIVINATION, ticks(180))));

        register("prussian_leap", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.JUMP, ticks(90), 1))
                .addEffect(() -> new MobEffectInstance(ModEffects.FEATHERFALL, ticks(90))));

        register("may_kiss", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks(135), 1))
                .addEffect(() -> new MobEffectInstance(ModEffects.KISS, ticks(135))));

        register("fleurs_du_mal", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.WITHER_AURA, ticks(180)))
                .addEffect(() -> new MobEffectInstance(MobEffects.WITHER, ticks(2))));

        register("breath_of_ancients", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.SNIFFER_SOUL, ticks(300)))
                .addEffect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, ticks(180))));

        register("the_sunward", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.SUNWARD, ticks(180))));

        register("spring_waltz", TeacupRegistry.TeacupData.create(4)
                .addEffect(cookeryEffect("warmth", 240, 0)));

        register("tender_thorns", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.THORNS, ticks(180))));

        register("coronation", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.LUCK, ticks(180)))
                .addEffect(() -> new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, ticks(60), 0)));

        register("voracious_urn", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.DIGESTION, ticks(240))));

        register("hanami_tale", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.PETAL_VEIL, ticks(180))));

        register("echo_of_the_end", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.ECHO, ticks(300))));

        register("vernal_awakening", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.SPROUT, ticks(180))));

        if (ModList.get().isLoaded("vanillabackport")) {

            register("the_gaze", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(ModEffects.GAZE, ticks(180))));

            register("as_you_wish", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(ModEffects.WISH, 1, 0)));

            register("springtime_stroll", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(ModEffects.FLOWER_PATH, ticks(180))));

            register("fleeting_bloom", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks(60)))
                    .addEffect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks(60)))
                    .addEffect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, ticks(60)))
                    .addEffect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks(60))));
        }
    }

    private static void register(String name, TeacupRegistry.TeacupData data) {
        REGISTRY.registerTeacupData(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, name), data);
    }

    private static Supplier<MobEffectInstance> cookeryEffect(String path, int seconds, int amplifier) {
        return () -> {
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(
                    ResourceLocation.fromNamespaceAndPath(COOKERY_ID, path));
            if (holder.isEmpty()) {
                throw new IllegalStateException(
                        "Kaleidoscope Cookery effect not found: " + COOKERY_ID + ":" + path);
            }
            return new MobEffectInstance(holder.get(), ticks(seconds), amplifier);
        };
    }
}
