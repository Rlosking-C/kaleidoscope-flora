package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The advancement layer of Kaleidoscope Flora: two custom criteria triggers
 * plus all the per-player bookkeeping that feeds them. The whole tree is 15
 * awards and reuses existing item textures as icons - no new art anywhere.
 *
 * <p>Tree design (user decision 2026-09-10, roadmap 7.3): one compact tree,
 * no one-drink-one-award. The visible collection line is "花饮初尝" (root,
 * first drink) -&gt; "半盏世界" (9 distinct drinks) -&gt; "满园春色" (every
 * drink there is); the hidden collector award "花开满园" fires when every
 * drink of this install is held in the inventory at the same moment. The
 * eleven hidden meme awards hang off {@link FloraEventTrigger} and are
 * fired from existing hooks in {@link FloraEvents} / {@link ModEffects}.</p>
 *
 * <p><b>Why a data attachment instead of pure JSON:</b> vanilla advancement
 * requirements are AND-of-ORs with no "at least N of M" operator, so
 * mid-tier collection counts (9 of 26) cannot be expressed with vanilla
 * triggers. The attachment adds the missing state, is copied on death, and
 * is saved with the player, so tasting progress survives both. The "drink
 * everything" award counts against the drinks actually registered in this
 * install, so it stays obtainable when VanillaBackport (and its 4 drinks)
 * is absent.</p>
 */
public final class FloraAdvancements {

    /** All criteria triggers of this mod, registered on the mod bus. */
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, KaleidoscopeFlora.MOD_ID);

    /** All data attachments of this mod, registered on the mod bus. */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KaleidoscopeFlora.MOD_ID);

    /** Named meme events, fired from gameplay hooks and matched by JSON. */
    public static final String EVENT_WIND_PLAY = "wind_play";
    public static final String EVENT_BREAD_OF_LIFE = "bread_of_life";
    public static final String EVENT_CHEATED_DEATH = "cheated_death";
    public static final String EVENT_GAZE_ABYSS = "gaze_abyss";
    public static final String EVENT_WATER_DANCER = "water_dancer";
    public static final String EVENT_ARROW_SHIELD = "arrow_shield";
    public static final String EVENT_CHASING_SUN = "chasing_sun";
    public static final String EVENT_GREEDY_URN = "greedy_urn";
    public static final String EVENT_FLOWERS_FOR_WINE = "flowers_for_wine";
    public static final String EVENT_BELIEVE_SPRING = "believe_spring";
    public static final String EVENT_LULLABY_DAWN = "lullaby_dawn";
    public static final String EVENT_FULL_BOUQUET = "full_bouquet";

    /**
     * The set of drink ids a player has ever tasted. Serialized with the
     * player, copied on death - a collection is never lost to a respawn.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<ResourceLocation>>> TASTED =
            ATTACHMENTS.register("tasted_drinks", () -> AttachmentType
                    .<Set<ResourceLocation>>builder(() -> new HashSet<>())
                    .serialize(ResourceLocation.CODEC.listOf().xmap(
                            list -> (Set<ResourceLocation>) new HashSet<>(list),
                            set -> new ArrayList<>(set)))
                    .copyOnDeath()
                    .build());

    /** The generic meme trigger: every hidden award matches on "event". */
    public static final DeferredHolder<CriterionTrigger<?>, FloraEventTrigger> FLORA_EVENT =
            TRIGGERS.register("flora_event", FloraEventTrigger::new);

    /** The collection trigger: counts distinct drinks ever tasted. */
    public static final DeferredHolder<CriterionTrigger<?>, DrinksTastedTrigger> DRINKS_TASTED =
            TRIGGERS.register("drinks_tasted", DrinksTastedTrigger::new);

    private FloraAdvancements() {
    }

    /** Registers triggers and attachments on the mod bus (mod constructor). */
    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
        ATTACHMENTS.register(modBus);
    }

    /**
     * Hunger level when the player began the drink currently in hand. The
     * "bread of life" meme must see the bar as it was BEFORE the cup: by
     * the time the Finish hook runs, the drink's food value has already
     * been applied, so the check would never see zero there.
     */
    private static final Map<UUID, Integer> FOOD_AT_SIP = new HashMap<>();

    /** Snapshots the hunger bar at the first sip of a flora drink. */
    public static void sipStarted(ServerPlayer player) {
        FOOD_AT_SIP.put(player.getUUID(), player.getFoodData().getFoodLevel());
    }

    /** Fires one named meme event to the trigger (no-op once earned). */
    public static void award(ServerPlayer player, String event) {
        FLORA_EVENT.get().award(player, event);
    }

    // ==================================================================
    // Drink finished: collection progress + the three drinking memes
    // ==================================================================

    /**
     * Called from {@code LivingEntityUseItemEvent.Finish} for every flora
     * drink a player finishes:
     * <ul>
     *   <li>collection: adds the drink to the tasted set and re-fires the
     *       count trigger (only new drinks trigger - re-tasting an old
     *       favourite would replay every milestone otherwise);</li>
     *   <li>"与风嬉戏": finished a When the Wind Rises while airborne -
     *       the maxim taken literally. fallDistance is maintained
     *       server-side for client-authoritative movement, so a mid-air
     *       drink is reliably detected; &gt;1 block means real descent
     *       (you cannot jump while using an item, so airborne = falling);</li>
     *   <li>"咖啡即是我们的面包": drank a First Bloom on an empty hunger
     *       bar (drinks carry no food value, so the bar is still at zero
     *       the moment the cup empties);</li>
     *   <li>"以花代酒": three DIFFERENT flower drinks within one minute -
     *       the toast the teetotaller raises instead of wine.</li>
     * </ul>
     */
    public static void drinkFinished(ServerPlayer player, ItemStack drink) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(drink.getItem());

        Set<ResourceLocation> tasted = player.getData(TASTED.get());
        if (tasted.add(id)) {
            DRINKS_TASTED.get().award(player, tasted.size(), totalDrinkCount());
        }

        if (id.getPath().equals("when_the_wind_rises") && player.fallDistance > 1.0F) {
            award(player, EVENT_WIND_PLAY);
        }
        // "Bread of life" judges the bar as it was BEFORE the cup: by the
        // time this Finish hook runs, the drink's food value has already
        // been applied, so a just-restocked bar would never read zero here.
        Integer hungerAtStart = FOOD_AT_SIP.remove(player.getUUID());
        boolean starving = hungerAtStart != null
                ? hungerAtStart == 0
                : player.getFoodData().getFoodLevel() == 0;
        if (id.getPath().equals("first_bloom") && starving) {
            award(player, EVENT_BREAD_OF_LIFE);
        }

        long now = player.level().getGameTime();
        List<DrinkStamp> recent = RECENT_DRINKS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        recent.removeIf(stamp -> now - stamp.time() > FLOWERS_FOR_WINE_WINDOW_TICKS);
        recent.add(new DrinkStamp(now, id));
        if (recent.stream().map(DrinkStamp::id).distinct().count() >= FLOWERS_FOR_WINE_CUPS) {
            award(player, EVENT_FLOWERS_FOR_WINE);
        }
    }

    /** One finished drink: when it was drunk and which one. */
    private record DrinkStamp(long time, ResourceLocation id) {
    }

    /** "以花代酒" window: three distinct cups inside one minute. */
    private static final long FLOWERS_FOR_WINE_WINDOW_TICKS = 60 * 20L;
    private static final long FLOWERS_FOR_WINE_CUPS = 3L;

    /**
     * The number of flora drinks registered in this install: 22 always,
     * 26 with VanillaBackport present. Computed lazily once (item registry
     * is long frozen by the time any drink is finished) and cached forever.
     */
    private static int totalDrinks;

    private static int totalDrinkCount() {
        if (totalDrinks == 0) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof TeacupItem
                        && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(KaleidoscopeFlora.MOD_ID)) {
                    totalDrinks++;
                }
            }
        }
        return totalDrinks;
    }

    // ==================================================================
    // Per-tick bookkeeping (called from FloraEvents every server tick)
    // ==================================================================

    /**
     * One server tick of advancement bookkeeping per player:
     * <ul>
     *   <li>clears the meme counters of effects that have lapsed, so a
     *       later cup starts a fresh "one effect" window;</li>
     *   <li>"轻功水上漂": accumulates Rosy Stride distance walked on the
     *       petal film; the counter resets the moment the feet leave the
     *       film (continuous crossing only, no wading breaks);</li>
     *   <li>"花开满园": once per second, checks whether every drink of
     *       this install is present in the inventory at the same time.</li>
     * </ul>
     */
    public static void tick(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // Meme counters live only as long as their effect does.
        if (!player.hasEffect(ModEffects.PETAL_VEIL)) {
            PETAL_BLOCKS.remove(uuid);
        }
        if (!player.hasEffect(ModEffects.DIGESTION)) {
            URN_DOUBLES.remove(uuid);
        }
        if (!player.hasEffect(ModEffects.SPROUT)) {
            SPROUT_CROPS.remove(uuid);
        }

        // Rosy Stride: continuous distance on the water film.
        Double walked = WATER_WALKED.get(uuid);
        if (!player.hasEffect(ModEffects.PETALWALK)) {
            WATER_WALKED.remove(uuid);
        } else if (FloraEvents.isOnPetalFilm(player)) {
            double[] step = FloraEvents.lastMove(player);
            double total = (walked == null ? 0.0 : walked) + Math.hypot(step[0], step[1]);
            if (total >= WATER_DANCER_BLOCKS) {
                award(player, EVENT_WATER_DANCER);
                WATER_WALKED.remove(uuid);
            } else {
                WATER_WALKED.put(uuid, total);
            }
        } else if (walked != null) {
            WATER_WALKED.remove(uuid); // back on land: the crossing broke
        }

        if (player.level().getGameTime() % 20 == 0 && holdingFullBouquet(player)) {
            award(player, EVENT_FULL_BOUQUET);
        }
    }

    /** "轻功水上漂" distance: fifty blocks of water in one crossing. */
    private static final double WATER_DANCER_BLOCKS = 50.0;

    /** "挡箭牌" threshold: five projectiles inside one Petal Veil. */
    private static final int ARROW_SHIELD_BLOCKS = 5;

    /** "贪心不足" threshold: three double drops inside one Voracious Urn. */
    private static final int GREEDY_URN_DOUBLES = 3;

    /** "相信整个春天" threshold: fifty crops ripened inside one Sprout. */
    private static final int BELIEVE_SPRING_CROPS = 50;

    private static final Map<UUID, Integer> PETAL_BLOCKS = new HashMap<>();
    private static final Map<UUID, Integer> URN_DOUBLES = new HashMap<>();
    private static final Map<UUID, Integer> SPROUT_CROPS = new HashMap<>();
    private static final Map<UUID, Double> WATER_WALKED = new HashMap<>();
    private static final Map<UUID, List<DrinkStamp>> RECENT_DRINKS = new HashMap<>();
    private static final Map<UUID, long[]> SUN_CHASE = new HashMap<>();

    // ==================================================================
    // One-shot meme counters, called from their gameplay hooks
    // ==================================================================

    /**
     * One projectile blocked by the Petal Veil ("挡箭牌"): five blocks
     * inside a single effect earn the award; the counter is cleared by
     * {@link #tick} once the veil lapses, so a later veil restarts at 0.
     */
    public static void petalVeilBlock(ServerPlayer player) {
        int blocks = PETAL_BLOCKS.merge(player.getUUID(), 1, Integer::sum);
        if (blocks >= ARROW_SHIELD_BLOCKS) {
            award(player, EVENT_ARROW_SHIELD);
            PETAL_BLOCKS.remove(player.getUUID());
        }
    }

    /** One doubled drop from the Voracious Urn ("贪心不足"): three per effect. */
    public static void urnDoubleDrop(ServerPlayer player) {
        int doubles = URN_DOUBLES.merge(player.getUUID(), 1, Integer::sum);
        if (doubles >= GREEDY_URN_DOUBLES) {
            award(player, EVENT_GREEDY_URN);
            URN_DOUBLES.remove(player.getUUID());
        }
    }

    /** One crop ripened by the Sprout effect ("相信整个春天"): fifty per effect. */
    public static void sproutCropRipened(ServerPlayer player) {
        int crops = SPROUT_CROPS.merge(player.getUUID(), 1, Integer::sum);
        if (crops >= BELIEVE_SPRING_CROPS) {
            award(player, EVENT_BELIEVE_SPRING);
            SPROUT_CROPS.remove(player.getUUID());
        }
    }

    /**
     * The Sunward's day form is active this tick ("夸父逐日"). Sunrise and
     * sunset are matched generously (the first resp. last thousand ticks of
     * the 12000-tick day); both inside the SAME game day - cups may be
     * re-drunk in between, but sleeping the night cannot join the two.
     */
    public static void sunwardDayForm(ServerPlayer player) {
        long dayTime = player.level().getDayTime();
        long day = dayTime / 24000L;
        long moment = dayTime % 24000L;
        long[] chase = SUN_CHASE.get(player.getUUID());
        if (chase == null || chase[0] != day) {
            chase = new long[]{day, 0L};
            SUN_CHASE.put(player.getUUID(), chase);
        }
        if (moment < 1200L) {
            chase[1] |= 1L; // met the sun at its rising
        }
        if (moment >= 11000L && moment < 12000L) {
            chase[1] |= 2L; // saw it off at its setting
        }
        if (chase[1] == 3L) {
            award(player, EVENT_CHASING_SUN);
            SUN_CHASE.remove(player.getUUID());
        }
    }

    /**
     * True when every drink of this install is in the player's inventory
     * (main storage + armour + offhand) at the same moment - the hidden
     * collector award "花开满园". Only checked once per second per player;
     * the scan is a single pass over at most 41 slots.
     */
    private static boolean holdingFullBouquet(ServerPlayer player) {
        Set<ResourceLocation> present = new HashSet<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isFloraDrink(stack)) {
                present.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
        }
        return present.size() >= totalDrinkCount();
    }

    /** True for the drink items this mod registers through Cookery. */
    public static boolean isFloraDrink(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TeacupItem
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(KaleidoscopeFlora.MOD_ID);
    }

    /** Frees every per-player map when the player logs out. */
    public static void clearPlayer(UUID uuid) {
        PETAL_BLOCKS.remove(uuid);
        URN_DOUBLES.remove(uuid);
        SPROUT_CROPS.remove(uuid);
        WATER_WALKED.remove(uuid);
        RECENT_DRINKS.remove(uuid);
        SUN_CHASE.remove(uuid);
        FOOD_AT_SIP.remove(uuid);
    }

    // ==================================================================
    // The two custom criteria triggers
    // ==================================================================

    /**
     * The generic meme trigger. One registry entry serves all eleven hidden
     * awards: each advancement JSON carries
     * {@code "conditions": {"event": "<name>"}} and gameplay code fires
     * the matching name. {@code award} is public because the inherited
     * {@code trigger(...)} is protected and must be reachable from this
     * outer class.
     */
    public static final class FloraEventTrigger extends SimpleCriterionTrigger<FloraEventTrigger.EventInstance> {
        @Override
        public Codec<EventInstance> codec() {
            return EventInstance.CODEC;
        }

        /** Fires one named meme event for this player. */
        public void award(ServerPlayer player, String event) {
            this.trigger(player, instance -> instance.event().equals(event));
        }

        public record EventInstance(String event) implements SimpleInstance {
            public static final Codec<EventInstance> CODEC = RecordCodecBuilder.create(b ->
                    b.group(Codec.STRING.fieldOf("event").forGetter(EventInstance::event)).apply(b, EventInstance::new));

            // SimpleInstance requires a player predicate; this trigger matches
            // by event name only, so there is never a player condition to test.
            @Override
            public Optional<ContextAwarePredicate> player() {
                return Optional.empty();
            }
        }
    }

    /**
     * The collection trigger: fires with the number of DISTINCT drinks the
     * player has ever tasted and the total registered in this install; an
     * instance matches when the player reached its count, or everything
     * there is when fewer drinks are installed (22 without
     * VanillaBackport) - "drink them all" must never become impossible.
     */
    public static final class DrinksTastedTrigger extends SimpleCriterionTrigger<DrinksTastedTrigger.EventInstance> {
        @Override
        public Codec<EventInstance> codec() {
            return EventInstance.CODEC;
        }

        /** Fires with the current distinct-drink count and install total. */
        public void award(ServerPlayer player, int tasted, int total) {
            this.trigger(player, instance -> tasted >= Math.min(instance.count(), total));
        }

        public record EventInstance(int count) implements SimpleInstance {
            public static final Codec<EventInstance> CODEC = RecordCodecBuilder.create(b ->
                    b.group(Codec.intRange(1, 64).fieldOf("count").forGetter(EventInstance::count))
                            .apply(b, EventInstance::new));

            // SimpleInstance requires a player predicate; this trigger counts
            // drinks from the attachment, so there is never a player condition.
            @Override
            public Optional<ContextAwarePredicate> player() {
                return Optional.empty();
            }
        }
    }
}
