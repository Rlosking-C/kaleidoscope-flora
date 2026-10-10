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
    // Flower cakes (v0.3.4)
    // ------------------------------------------------------------------

    /**
     * Extra mid-air jumps the Dew Flower Cake grants (1 = a literal double jump).
     *
     * <p>The design asked for this to live in the config as the <b>default</b>,
     * with the effect's own amplifier able to override it per item. That is how
     * it is wired: {@code FlowerCakeItem} reads this value when it applies the
     * effect, so the number travels inside the effect instance from then on and
     * the jump logic itself never reads the config. A future triple-jump item is
     * therefore a data change - apply the effect with amplifier 2 - and not a
     * code change.</p>
     *
     * <p>0 disables the extra jump while keeping the landing shockwave, which is
     * the natural way to run the cake as shockwave-only.</p>
     */
    public final ModConfigSpec.IntValue doubleJumpCount;

    /** Flower Perch (v0.4.0): growth time and the bloom petal shower. */
    public final ModConfigSpec.DoubleValue perchGrowthMinutesMin;
    public final ModConfigSpec.DoubleValue perchGrowthMinutesMax;
    public final ModConfigSpec.DoubleValue perchAromaDensity;
    public final ModConfigSpec.DoubleValue perchAromaRange;

    // ------------------------------------------------------------------
    // Farming
    // ------------------------------------------------------------------
    //
    // Empty since v0.3.4. The category's only option, harvestMaxDropMultiplier,
    // went away with the time-based Autumn Serenade effect - see the note in the
    // constructor. Left as a marked placeholder rather than deleted so the next
    // reader sees the removal was deliberate.

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

        builder.comment("Kaleidoscope Flora - Flower Perch Settings",
                        "森罗物语：花香四溢 - 花居设置")
                .push("flower_perch");

        perchGrowthMinutesMin = builder
                .comment("Shortest time a planted flower takes to reach full bloom, in minutes.",
                        "种植到盛放所需的最短时间（分钟）。")
                .defineInRange("perchGrowthMinutesMin", 3.0, 0.05, 60.0);

        perchGrowthMinutesMax = builder
                .comment("Longest time a planted flower takes to reach full bloom, in minutes.",
                        "种植到盛放所需的最长时间（分钟）；若小于最短值，按最短值处理。")
                .defineInRange("perchGrowthMinutesMax", 5.0, 0.05, 60.0);

        perchAromaDensity = builder
                .comment("Bloom petal particles per tick, per perch - the DEFAULT look preset.",
                        "0 disables the shower; the in-game look presets are 0.5 / 1.5 / 3.0.",
                        "每座盛放花居每 tick 飘落的花瓣数（默认观感档）。0 = 关闭；档位为 0.5 / 1.5 / 3.0。")
                .defineInRange("perchAromaDensity", 1.5, 0.0, 4.0);

        perchAromaRange = builder
                .comment("Horizontal radius of the petal shower, in blocks.",
                        "花瓣飘落的水平半径（格）。")
                .defineInRange("perchAromaRange", 16.0, 0.5, 32.0);

        builder.pop();

        // The "farming" category held exactly one option, harvestMaxDropMultiplier,
        // and v0.3.4 removed it: the Autumn Serenade effect is charge-based now
        // (20 harvests at a fixed x2-x3), so a configurable upper bound has
        // nothing left to control. Keeping a dead knob in the config screen would
        // be worse than removing it - players would move it and see no change.
        // The category goes with it rather than standing empty.
        //
        // Existing config files keep the stale key on disk; NeoForge ignores keys
        // that the spec no longer defines, so it is harmless and self-cleans if the
        // file is ever rewritten.
    }

    // ------------------------------------------------------------------
    // Convenience getters
    // ------------------------------------------------------------------

    public static double effectDurationMultiplier() { return CONFIG.effectDurationMultiplier.get(); }
    public static double auraRangeMultiplier() { return CONFIG.auraRangeMultiplier.get(); }
    public static double vampiricHealPercent() { return CONFIG.vampiricHealPercent.get(); }
    public static int doubleJumpCount() { return CONFIG.doubleJumpCount.get(); }

    public static double perchGrowthMinutesMin() { return CONFIG.perchGrowthMinutesMin.get(); }

    /** Never below the minimum: Mth.nextDouble needs low < high or it throws. */
    public static double perchGrowthMinutesMax() {
        return Math.max(CONFIG.perchGrowthMinutesMax.get(), CONFIG.perchGrowthMinutesMin.get());
    }

    public static double perchAromaDensity() { return CONFIG.perchAromaDensity.get(); }

    public static double perchAromaRange() { return CONFIG.perchAromaRange.get(); }
}
