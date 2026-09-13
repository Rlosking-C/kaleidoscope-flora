package com.rlosking.flora;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-synced configuration for Kaleidoscope Flora.
 *
 * <p>Exposes the most impactful gameplay knobs so server admins can tune
 * the mod without editing code or JSON. All values are {@link
 * ModConfig.Type#COMMON COMMON}: loaded on both sides, not synced (each
 * side reads its own file), so client and server should agree.</p>
 */
public final class FloraConfig {

    public static final FloraConfig CONFIG;
    public static final ModConfigSpec SPEC;

    static {
        var pair = new ModConfigSpec.Builder().configure(FloraConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    // ------------------------------------------------------------------
    // Effects
    // ------------------------------------------------------------------

    /** Multiplier applied to every drink's effect duration (1.0 = normal). */
    public final ModConfigSpec.DoubleValue effectDurationMultiplier;

    /** Multiplier applied to all aura radii (Drowsy, Wither Aura, Gaze, Fadeaway). */
    public final ModConfigSpec.DoubleValue auraRangeMultiplier;

    // ------------------------------------------------------------------
    // Combat
    // ------------------------------------------------------------------

    /** Percent of melee damage healed by the Crimson Heartbeat vampiric effect. */
    public final ModConfigSpec.DoubleValue vampiricHealPercent;

    // ------------------------------------------------------------------
    // Farming
    // ------------------------------------------------------------------

    /** Maximum crop-drop multiplier for the Autumn Serenade harvest effect (4 = up to 4x). */
    public final ModConfigSpec.IntValue harvestMaxDropMultiplier;

    private FloraConfig(ModConfigSpec.Builder builder) {
        builder.comment("Kaleidoscope Flora - Gameplay Settings",
                        "森罗物语：花香四溢 - 玩法设置")
                .push("effects");

        effectDurationMultiplier = builder
                .comment("Multiplier for all drink effect durations.",
                        "1.0 = normal, 0.5 = half duration, 2.0 = double duration.",
                        "全部花饮效果时长的倍率。1.0 = 正常，0.5 = 一半时长，2.0 = 两倍时长。")
                .defineInRange("effectDurationMultiplier", 1.0, 0.25, 4.0);

        auraRangeMultiplier = builder
                .comment("Multiplier for aura effect ranges.",
                        "Affects Drowsy, Wither Aura, Gaze, and Fadeaway.",
                        "1.0 = normal, 0.5 = half range, 2.0 = double range.",
                        "光环类效果范围的倍率（安眠曲、凋零光环、凝视、消隐）。1.0 = 正常，0.5 = 一半范围，2.0 = 两倍范围。")
                .defineInRange("auraRangeMultiplier", 1.0, 0.25, 3.0);

        builder.pop();

        builder.comment("Kaleidoscope Flora - Combat Settings",
                        "森罗物语：花香四溢 - 战斗设置")
                .push("combat");

        vampiricHealPercent = builder
                .comment("Percent of melee damage healed by the Crimson Heartbeat vampiric effect.",
                        "20 = 20% of damage as healing (default).",
                        "绯色心跳吸血效果：近战伤害转化为生命回复的百分比。20 = 吸血 20%（默认）。")
                .defineInRange("vampiricHealPercent", 20.0, 0.0, 100.0);

        builder.pop();

        builder.comment("Kaleidoscope Flora - Farming Settings",
                        "森罗物语：花香四溢 - 农耕设置")
                .push("farming");

        harvestMaxDropMultiplier = builder
                .comment("Maximum crop-drop multiplier for the Autumn Serenade harvest effect.",
                        "4 = up to 4x drops (default), 2 = up to 2x, 8 = up to 8x.",
                        "秋日小夜曲收割效果的掉落上限倍率。4 = 最高 4 倍掉落（默认），2 = 最高 2 倍，8 = 最高 8 倍。")
                .defineInRange("harvestMaxDropMultiplier", 4, 2, 8);

        builder.pop();
    }

    // ------------------------------------------------------------------
    // Convenience getters
    // ------------------------------------------------------------------

    public static double effectDurationMultiplier() { return CONFIG.effectDurationMultiplier.get(); }
    public static double auraRangeMultiplier() { return CONFIG.auraRangeMultiplier.get(); }
    public static double vampiricHealPercent() { return CONFIG.vampiricHealPercent.get(); }
    public static int harvestMaxDropMultiplier() { return CONFIG.harvestMaxDropMultiplier.get(); }
}
