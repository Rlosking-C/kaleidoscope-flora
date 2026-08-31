package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.fml.ModList;

import java.util.function.Supplier;

/**
 * The catalogue of all 26 flower drinks this addon ships.
 *
 * <p><b>How this works:</b> Cookery exposes a public, mutable
 * {@code TeacupRegistry.TEACUP_DATA_MAP}. Every entry gets a teacup block, a
 * drink item and a creative tab entry registered automatically by Cookery
 * itself. The drink item already behaves like an official tea: hold
 * right-click to drink (32 ticks), returns an empty cup, shows effect
 * tooltips. We only describe WHAT a drink does, not HOW it works.</p>
 *
 * <p><b>Brewing is stockpot-based (author decision, 2026-08-29):</b> all
 * drinks are brewed in Cookery's STOCKPOT, not the teapot. The stockpot
 * matches 1-9 ingredients in any order, supports a soup base (water by
 * default; lava and even live fish buckets exist) and hands out one teacup
 * per empty cup used as carrier. Because the teapot is not involved at all,
 * there is zero overlap with Cookery's own teapot recipes - no overrides,
 * no ingredient tags, no conflicts.</p>
 *
 * <p><b>HOW TO ADD A NEW DRINK (checklist):</b>
 * <ol>
 *   <li>Add a {@code register(...)} call below with the drink id and its
 *       effects. Vanilla effects via {@link MobEffects}, Cookery effects via
 *       {@link #cookeryEffect(String, int, int)}, this mod's effects via the
 *       holders in {@link ModEffects}.</li>
 *   <li>Stockpot recipe at
 *       {@code data/kaleidoscope_flora/recipe/stockpot/<id>.json}
 *       (copy any existing one; the carrier is always the empty cup).</li>
 *   <li>Item model at {@code assets/kaleidoscope_flora/models/item/<id>.json}
 *       plus a 16x16 texture at
 *       {@code assets/kaleidoscope_flora/textures/item/<id>.png}.</li>
 *   <li>Block assets: copy the whole folder
 *       {@code assets/kaleidoscope_flora/models/block/teacup/<any drink>/}
 *       (10 count models), copy a blockstate JSON, and add a 32x32 texture at
 *       {@code assets/kaleidoscope_flora/textures/block/teacup/<id>.png}.
 *       All model JSONs are geometry-identical; only the texture path
 *       differs, so search-and-replace is enough (tools/generate.ps1 does
 *       all of this).</li>
 *   <li>Lang entries in en_us.json / zh_cn.json:
 *       {@code block.kaleidoscope_flora.<id>} (name) and
 *       {@code tooltip.kaleidoscope_flora.<id>.maxim} (the flavour quote
 *       shown on the item tooltip).</li>
 * </ol>
 *
 * <p><b>Why there is no hunger value:</b> author decision 2026-08-29. Cookery
 * drink items carry no FoodProperties; drinks are pure effect carriers.</p>
 */
public final class FloraDrinks {
    private static final String COOKERY_ID = "kaleidoscope_cookery";

    /** One shared registry instance; its register method is instance-based. */
    private static final TeacupRegistry REGISTRY = new TeacupRegistry();

    private FloraDrinks() {
    }

    public static void registerAll() {
        // --------------------------------------------------------------
        // Vanilla 1.21.1 flowers (brewable with no other mods installed)
        // --------------------------------------------------------------

        // Dandelion "When the Wind Rises": instant purge of all harmful
        // effects, then a slow, gentle descent. Maxim: Gibran (Lebanon).
        register("when_the_wind_rises", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.PURGE, 1, 0))
                .addEffect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 90 * 20)));

        // Poppy "Lullaby": drowsiness aura - hostiles slow to a crawl,
        // phantoms lose their lock, insomnia resets. Maxim: Li Yu (China).
        register("lullaby", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.DROWSY, 3 * 60 * 20)));

        // Blue orchid "First Bloom": food eaten restores 50% more.
        // Maxim: Amharic proverb (Ethiopia).
        register("first_bloom", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.TASTEBLOOM, 5 * 60 * 20)));

        // Allium "Fire Waltz": fire resistance, and melee hits ignite.
        // Maxim: Dangun myth (Korea).
        register("fire_waltz", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4 * 60 * 20))
                .addEffect(() -> new MobEffectInstance(ModEffects.FIREBRAND, 3 * 60 * 20)));

        // Azure bluet "The Unnoticed": invisibility plus instant aggro drop.
        // Maxim: Emily Dickinson (USA).
        register("the_unnoticed", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.INVISIBILITY, 60 * 20))
                .addEffect(() -> new MobEffectInstance(ModEffects.FADEAWAY, 1, 0)));

        // Red tulip "Crimson Heartbeat": 15% lifesteal on melee.
        // Maxim: Persian legend of Farhad (Persia).
        register("crimson_heartbeat", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.VAMPIRIC, 3 * 60 * 20)));

        // Orange tulip "Autumn Serenade": haste and double harvest drops.
        // Maxim: Hesiod, Works and Days (ancient Greece).
        register("autumn_serenade", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.HARVEST, 3 * 60 * 20))
                .addEffect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3 * 60 * 20)));

        // White tulip "Absolution": harmful effects cannot be applied.
        // Maxim: Gandhi (India).
        register("absolution", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.ABSOLVE, 3 * 60 * 20)));

        // Pink tulip "Rosy Stride": walk on water, leave petals behind.
        // Maxim: Andersen, Thumbelina (Denmark).
        register("rosy_stride", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.PETALWALK, 3 * 60 * 20)));

        // Oxeye daisy "Loves Me Not": regeneration plus a divination roll
        // every 8 seconds. Maxim: European petal divination.
        register("loves_me_not", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.REGENERATION, 90 * 20))
                .addEffect(() -> new MobEffectInstance(ModEffects.DIVINATION, 3 * 60 * 20)));

        // Cornflower "The Prussian Leap": jump boost II and no fall damage.
        // Maxim: Leonardo da Vinci (Italy).
        register("prussian_leap", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.JUMP, 3 * 60 * 20, 1))
                .addEffect(() -> new MobEffectInstance(ModEffects.FEATHERFALL, 3 * 60 * 20)));

        // Lily of the valley "May Kiss": max hearts up; being hurt releases
        // a poison cloud. Maxim: Song of Songs (ancient Israel).
        register("may_kiss", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 5 * 60 * 20, 1))
                .addEffect(() -> new MobEffectInstance(ModEffects.KISS, 5 * 60 * 20)));

        // Wither rose "Les Fleurs du Mal": wither aura around the drinker;
        // the price of the cup is 2 seconds of wither. Maxim: Baudelaire.
        register("fleurs_du_mal", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.WITHER_AURA, 3 * 60 * 20))
                .addEffect(() -> new MobEffectInstance(MobEffects.WITHER, 2 * 20)));

        // Torchflower "Breath of the Ancients": night vision plus the
        // sniffer soul - brush dirt family for ancient relics, paying for
        // every find with buff duration. Maxim: Egyptian Book of the Dead.
        register("breath_of_ancients", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.SNIFFER_SOUL, 5 * 60 * 20))
                .addEffect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 5 * 60 * 20)));

        // Sunflower "The Sunward": regeneration under the open day sky,
        // glowing through the night. Maxim: Van Gogh (Netherlands).
        register("the_sunward", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.SUNWARD, 3 * 60 * 20)));

        // Lilac "Spring Waltz": Cookery's own warmth (freeze immunity).
        // Maxim: Rilke (Austria).
        register("spring_waltz", TeacupRegistry.TeacupData.create(4)
                .addEffect(cookeryEffect("warmth", 4 * 60 * 20, 0)));

        // Rose bush "Tender Thorns": attackers take 2 hearts + weakness.
        // Maxim: Robert Burns (Scotland).
        register("tender_thorns", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.THORNS, 3 * 60 * 20)));

        // Peony "Coronation": luck - the hidden vanilla effect, this is its
        // only regular source. Maxim: Inca saying.
        register("coronation", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(MobEffects.LUCK, 5 * 60 * 20)));

        // Pitcher plant "The Voracious Urn": kills count as +1 looting.
        // Maxim: Charles Darwin letter (UK).
        register("voracious_urn", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.DIGESTION, 4 * 60 * 20)));

        // Pink petals "Hanami Tale": petals orbit and block projectiles,
        // spending 10 seconds of duration per block. Maxim: Ryokan (Japan).
        register("hanami_tale", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.PETAL_VEIL, 3 * 60 * 20)));

        // Chorus flower "Echo of the End": cheat death once - teleport and
        // survive on half a heart. Maxim: Gilgamesh (Mesopotamia).
        register("echo_of_the_end", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.ECHO, 5 * 60 * 20)));

        // Spore blossom "Vernal Awakening": crops nearby ripen randomly.
        // Maxim: farming proverb (anonymous).
        register("vernal_awakening", TeacupRegistry.TeacupData.create(4)
                .addEffect(() -> new MobEffectInstance(ModEffects.SPROUT, 3 * 60 * 20)));

        // --------------------------------------------------------------
        // Newer flowers (1.21.4+): these flowers simply do not exist on
        // 1.21.1, so the drinks are ONLY registered when VanillaBackport
        // (or another mod providing them) is installed. The mod id is
        // queried here because our mod constructor is allowed to touch
        // ModList - registration happens later, after ALL mod
        // constructors ran, so the check is always up to date.
        // --------------------------------------------------------------
        if (ModList.get().isLoaded("vanillabackport")) {

            // Eyeblossom "The Gaze": everything alive in 25 blocks glows
            // through walls. Maxim: Nietzsche, Beyond Good and Evil.
            register("the_gaze", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(ModEffects.GAZE, 3 * 60 * 20)));

            // Golden dandelion "As You Wish": one random tier-II blessing.
            // Maxim: One Thousand and One Nights.
            register("as_you_wish", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(ModEffects.WISH, 1, 0)));

            // Wildflowers "Springtime Stroll": grass you cross blooms into
            // real flowers. Maxim: Antonio Machado, Campos de Castilla.
            register("springtime_stroll", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(ModEffects.FLOWER_PATH, 3 * 60 * 20)));

            // Cactus flower "Fleeting Bloom": all four tier-I buffs, for one
            // minute only. Maxim: Aztec poetry.
            register("fleeting_bloom", TeacupRegistry.TeacupData.create(4)
                    .addEffect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20))
                    .addEffect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20))
                    .addEffect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 60 * 20))
                    .addEffect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20)));
        }
    }

    /**
     * Pushes one drink into Cookery's teacup map under this mod's namespace.
     * Cookery auto-registers the block/item/tab entry for it during its
     * RegisterEvent phase, which happens after every mod's constructor.
     */
    private static void register(String name, TeacupRegistry.TeacupData data) {
        REGISTRY.registerTeacupData(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, name), data);
    }

    /**
     * Looks up one of Cookery's custom MobEffects at recipe-apply time (the
     * supplier deliberately defers the lookup so it only runs in a live
     * world, after all registries are frozen).
     *
     * @param path          effect id inside the kaleidoscope_cookery namespace,
     *                      e.g. "vigor", "warmth", "instant_smelting"
     * @param durationTicks effect duration in ticks (20 ticks = 1 second)
     * @param amplifier     effect level, 0 = level I
     */
    private static Supplier<MobEffectInstance> cookeryEffect(String path, int durationTicks, int amplifier) {
        return () -> {
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(
                    ResourceLocation.fromNamespaceAndPath(COOKERY_ID, path));
            if (holder.isEmpty()) {
                throw new IllegalStateException(
                        "Kaleidoscope Cookery effect not found: " + COOKERY_ID + ":" + path);
            }
            return new MobEffectInstance(holder.get(), durationTicks, amplifier);
        };
    }
}
