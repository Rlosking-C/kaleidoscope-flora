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
import net.minecraft.world.item.BlockItem;
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

public final class FloraAdvancements {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, KaleidoscopeFlora.MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KaleidoscopeFlora.MOD_ID);

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
    public static final String EVENT_RIDE_THE_WIND = "ride_the_wind";

    public static final String EVENT_RAW_PIN = "raw_pin";
    public static final String EVENT_DEW_IMPACT = "dew_impact";
    public static final String EVENT_RAW_THREE_PINS = "raw_three_pins";
    public static final String EVENT_DEW_HIGH_IMPACT = "dew_high_impact";

    private static final Map<UUID, List<Object[]>> PIN_WINDOW = new HashMap<>();

    private static final int PIN_WINDOW_TICKS = 20;

    private static final int PIN_WINDOW_TARGETS = 3;

    private static final double DEW_HIGH_IMPACT_DROP = 20.0;

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<ResourceLocation>>> TASTED =
            ATTACHMENTS.register("tasted_drinks", () -> AttachmentType
                    .<Set<ResourceLocation>>builder(() -> new HashSet<>())
                    .serialize(ResourceLocation.CODEC.listOf().xmap(
                            list -> (Set<ResourceLocation>) new HashSet<>(list),
                            set -> new ArrayList<>(set)))
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<CriterionTrigger<?>, FloraEventTrigger> FLORA_EVENT =
            TRIGGERS.register("flora_event", FloraEventTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, DrinksTastedTrigger> DRINKS_TASTED =
            TRIGGERS.register("drinks_tasted", DrinksTastedTrigger::new);

    private FloraAdvancements() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
        ATTACHMENTS.register(modBus);
    }

    private static final Map<UUID, Integer> FOOD_AT_SIP = new HashMap<>();

    public static void sipStarted(ServerPlayer player) {
        FOOD_AT_SIP.put(player.getUUID(), player.getFoodData().getFoodLevel());
    }

    public static void award(ServerPlayer player, String event) {
        FLORA_EVENT.get().award(player, event);
    }

    public static void drinkFinished(ServerPlayer player, ItemStack drink) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(drink.getItem());

        Set<ResourceLocation> tasted = player.getData(TASTED.get());
        if (tasted.add(id)) {
            DRINKS_TASTED.get().award(player, tasted.size(), totalDrinkCount());
        }

        if (id.getPath().equals("when_the_wind_rises") && player.fallDistance > 1.0F) {
            award(player, EVENT_WIND_PLAY);
        }
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

    private record DrinkStamp(long time, ResourceLocation id) {
    }

    private static final long FLOWERS_FOR_WINE_WINDOW_TICKS = 60 * 20L;
    private static final long FLOWERS_FOR_WINE_CUPS = 3L;

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

    public static void tick(ServerPlayer player) {
        UUID uuid = player.getUUID();

        if (!player.hasEffect(ModEffects.PETAL_VEIL)) {
            PETAL_BLOCKS.remove(uuid);
        }
        if (!player.hasEffect(ModEffects.DIGESTION)) {
            URN_DOUBLES.remove(uuid);
        }
        if (!player.hasEffect(ModEffects.SPROUT)) {
            SPROUT_CROPS.remove(uuid);
        }

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
            WATER_WALKED.remove(uuid);
        }

        Double flown = AIR_SWUM.get(uuid);
        if (!ModEffects.isAirborne(player)) {
            AIR_SWUM.remove(uuid);
        } else {
            double[] step = FloraEvents.lastMove(player);
            double total = (flown == null ? 0.0 : flown) + Math.hypot(step[0], step[1]);
            if (total >= RIDE_THE_WIND_BLOCKS) {
                award(player, EVENT_RIDE_THE_WIND);
                AIR_SWUM.remove(uuid);
            } else {
                AIR_SWUM.put(uuid, total);
            }
        }

        if (player.level().getGameTime() % 20 == 0 && holdingFullBouquet(player)) {
            award(player, EVENT_FULL_BOUQUET);
        }
    }

    private static final double WATER_DANCER_BLOCKS = 50.0;

    private static final double RIDE_THE_WIND_BLOCKS = 100.0;

    private static final int ARROW_SHIELD_BLOCKS = 5;

    private static final int GREEDY_URN_DOUBLES = 3;

    private static final int BELIEVE_SPRING_CROPS = 50;

    private static final Map<UUID, Integer> PETAL_BLOCKS = new HashMap<>();
    private static final Map<UUID, Integer> URN_DOUBLES = new HashMap<>();
    private static final Map<UUID, Integer> SPROUT_CROPS = new HashMap<>();
    private static final Map<UUID, Double> WATER_WALKED = new HashMap<>();
    private static final Map<UUID, Double> AIR_SWUM = new HashMap<>();
    private static final Map<UUID, List<DrinkStamp>> RECENT_DRINKS = new HashMap<>();
    private static final Map<UUID, long[]> SUN_CHASE = new HashMap<>();

    public static void petalVeilBlock(ServerPlayer player) {
        int blocks = PETAL_BLOCKS.merge(player.getUUID(), 1, Integer::sum);
        if (blocks >= ARROW_SHIELD_BLOCKS) {
            award(player, EVENT_ARROW_SHIELD);
            PETAL_BLOCKS.remove(player.getUUID());
        }
    }

    public static void urnDoubleDrop(ServerPlayer player) {
        int doubles = URN_DOUBLES.merge(player.getUUID(), 1, Integer::sum);
        if (doubles >= GREEDY_URN_DOUBLES) {
            award(player, EVENT_GREEDY_URN);
            URN_DOUBLES.remove(player.getUUID());
        }
    }

    public static void rawFlowerCakePin(ServerPlayer shooter, UUID targetId) {
        award(shooter, EVENT_RAW_PIN);
        long now = shooter.level().getGameTime();
        List<Object[]> window = PIN_WINDOW.computeIfAbsent(shooter.getUUID(), k -> new ArrayList<>());
        window.removeIf(entry -> now - (long) entry[1] > PIN_WINDOW_TICKS);
        boolean alreadyIn = window.stream().anyMatch(entry -> entry[0].equals(targetId));
        if (!alreadyIn) {
            window.add(new Object[]{targetId, now});
        }
        long distinct = window.stream().map(entry -> entry[0]).distinct().count();
        if (distinct >= PIN_WINDOW_TARGETS) {
            award(shooter, EVENT_RAW_THREE_PINS);
            window.clear();
        }
    }

    public static void dewImpact(ServerPlayer player, double drop) {
        award(player, EVENT_DEW_IMPACT);
        if (drop >= DEW_HIGH_IMPACT_DROP) {
            award(player, EVENT_DEW_HIGH_IMPACT);
        }
    }

    public static void sproutCropRipened(ServerPlayer player) {
        int crops = SPROUT_CROPS.merge(player.getUUID(), 1, Integer::sum);
        if (crops >= BELIEVE_SPRING_CROPS) {
            award(player, EVENT_BELIEVE_SPRING);
            SPROUT_CROPS.remove(player.getUUID());
        }
    }

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
            chase[1] |= 1L;
        }
        if (moment >= 11000L && moment < 12000L) {
            chase[1] |= 2L;
        }
        if (chase[1] == 3L) {
            award(player, EVENT_CHASING_SUN);
            SUN_CHASE.remove(player.getUUID());
        }
    }

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

    public static boolean isFloraDrink(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TeacupItem
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(KaleidoscopeFlora.MOD_ID);
    }

    public static boolean isFloraCake(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof FlowerCakeBlock;
    }

    public static void clearPlayer(UUID uuid) {
        PETAL_BLOCKS.remove(uuid);
        URN_DOUBLES.remove(uuid);
        SPROUT_CROPS.remove(uuid);
        WATER_WALKED.remove(uuid);
        AIR_SWUM.remove(uuid);
        RECENT_DRINKS.remove(uuid);
        SUN_CHASE.remove(uuid);
        FOOD_AT_SIP.remove(uuid);
        PIN_WINDOW.remove(uuid);
    }

    public static final class FloraEventTrigger extends SimpleCriterionTrigger<FloraEventTrigger.EventInstance> {
        @Override
        public Codec<EventInstance> codec() {
            return EventInstance.CODEC;
        }

        public void award(ServerPlayer player, String event) {
            this.trigger(player, instance -> instance.event().equals(event));
        }

        public record EventInstance(String event) implements SimpleInstance {
            public static final Codec<EventInstance> CODEC = RecordCodecBuilder.create(b ->
                    b.group(Codec.STRING.fieldOf("event").forGetter(EventInstance::event)).apply(b, EventInstance::new));

            @Override
            public Optional<ContextAwarePredicate> player() {
                return Optional.empty();
            }
        }
    }

    public static final class DrinksTastedTrigger extends SimpleCriterionTrigger<DrinksTastedTrigger.EventInstance> {
        @Override
        public Codec<EventInstance> codec() {
            return EventInstance.CODEC;
        }

        public void award(ServerPlayer player, int tasted, int total) {
            this.trigger(player, instance -> tasted >= Math.min(instance.count(), total));
        }

        public record EventInstance(int count) implements SimpleInstance {
            public static final Codec<EventInstance> CODEC = RecordCodecBuilder.create(b ->
                    b.group(Codec.intRange(1, 64).fieldOf("count").forGetter(EventInstance::count))
                            .apply(b, EventInstance::new));

            @Override
            public Optional<ContextAwarePredicate> player() {
                return Optional.empty();
            }
        }
    }
}
