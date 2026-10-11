package com.rlosking.flora;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FloraConfig {

    public static final FloraConfig CONFIG;
    public static final ModConfigSpec SPEC;

    static {
        var pair = new ModConfigSpec.Builder().configure(FloraConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.DoubleValue effectDurationMultiplier;

    public final ModConfigSpec.DoubleValue auraRangeMultiplier;

    public final ModConfigSpec.DoubleValue vampiricHealPercent;

    public final ModConfigSpec.IntValue doubleJumpCount;

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

        builder.comment("Kaleidoscope Flora - Flower Cake Settings",
                        "森罗物语：花香四溢 - 鲜花饼设置")
                .push("flower_cakes");

        doubleJumpCount = builder
                .comment("Extra mid-air jumps granted by the Dew Flower Cake.",
                        "1 = a literal double jump (default); 0 = no extra jump, shockwave only; up to 8.",
                        "清露鲜花饼提供的空中额外跳跃次数。1 = 字面意义的二段跳（默认）；0 = 不提供额外跳跃，只保留落地冲击；最高 8。")
                .defineInRange("doubleJumpCount", 1, 0, 8);

        builder.pop();

    }

    public static double effectDurationMultiplier() { return CONFIG.effectDurationMultiplier.get(); }
    public static double auraRangeMultiplier() { return CONFIG.auraRangeMultiplier.get(); }
    public static double vampiricHealPercent() { return CONFIG.vampiricHealPercent.get(); }
    public static int doubleJumpCount() { return CONFIG.doubleJumpCount.get(); }
}
