package com.rlosking.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The flower tea bags: one bag per drink, crafted from the flower itself
 * plus Cookery's dried tea leaves.
 *
 * <p><b>The chain, as of 2026-09-23.</b> The flower goes
 * straight into a crafting grid with {@code kaleidoscope_cookery:dried_tea_leaves}
 * in the middle and the rest of that drink's old stockpot recipe around it; the
 * finished bag brews a full 12-cup pot in the teapot (over water, except Hanami
 * Tale, which is brewed in milk). There is no dried-flower step any more: the 25
 * {@code dried_*} items, their textures and models, the Bamboo Tray drying
 * recipes and the whole {@code recipe/drying/} folder were deleted, and
 * {@code tools/gen_tea_chain.py} both regenerates and re-prunes them.</p>
 *
 * <p><b>Why these are plain items.</b> Nothing here has behaviour: the bag
 * recipe is a vanilla shaped recipe, brewing is a Cookery recipe type, and the
 * drinks themselves belong to Cookery's teacup registry. So this class owns only
 * the id list and its registration - the recipes, item models and language keys
 * all come from the same table in {@code tools/gen_tea_chain.py}.</p>
 *
 * <p><b>Two asymmetries, both author decisions:</b></p>
 * <ul>
 *   <li><b>Eyeblossoms share one bag</b> (2026-09-22). The closed and the open
 *       eyeblossom are two flowers but one drink ("The Gaze"), so there is a
 *       single {@code eyeblossom_tea_bag} and TWO recipes make it - one from each
 *       flower - so neither flower becomes dead content.</li>
 *   <li><b>"As You Wish" is built on gold</b> (2026-09-23). The golden dandelion
 *       it is named for has not been ported by VanillaBackport yet, so its
 *       "flower" is {@code minecraft:gold_nugget} and its bag is
 *       {@code golden_dandelion_tea_bag}. Swap that one item id in the
 *       generator's table once the flower exists.</li>
 * </ul>
 *
 * <p>The list is also the creative tab's order.</p>
 */
public final class FloraTeas {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KaleidoscopeFlora.MOD_ID);

    /**
     * Flowers this chain covers, in catalogue order. Same order as
     * {@code tools/gen_tea_chain.py}'s TABLE - keep the two in step.
     * {@code golden_dandelion} is not a real flower yet: it stands for the
     * golden dandelion and is crafted from gold nuggets.
     */
    private static final List<String> FLOWERS = List.of(
            "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
            "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy",
            "cornflower", "lily_of_the_valley", "wither_rose", "torchflower", "sunflower",
            "lilac", "rose_bush", "peony", "pitcher_plant", "pink_petals",
            "chorus_flower", "spore_blossom",
            // VanillaBackport flowers - registered only when VB is loaded.
            "wildflowers", "cactus_flower", "eyeblossom", "golden_dandelion");

    /**
     * The flowers only VanillaBackport provides. Without VB neither the flowers
     * nor their recipes exist, so registering the bags anyway would leave
     * unobtainable entries sitting in the creative tab - the same reasoning that
     * gates the VB drinks.
     */
    private static final List<String> VB_FLOWERS =
            List.of("wildflowers", "cactus_flower", "eyeblossom", "golden_dandelion");

    /** Registered tea bags, in the creative tab's order. */
    private static final List<DeferredHolder<Item, Item>> TEAS = new ArrayList<>();

    /** Registers every tea bag this install offers. */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        boolean backport = ModList.get().isLoaded("vanillabackport");

        for (String flower : available(backport)) {
            TEAS.add(ITEMS.register(flower + "_tea_bag", () -> new Item(new Item.Properties())));
        }
        KaleidoscopeFlora.LOGGER.info("Flower tea bags: {} items ({} flowers{})",
                TEAS.size(), available(backport).size(),
                backport ? ", VanillaBackport loaded" : "");
    }

    private static List<String> available(boolean backport) {
        return backport ? FLOWERS : FLOWERS.stream().filter(f -> !VB_FLOWERS.contains(f)).toList();
    }

    /** Every tea bag in this install, in creative-tab order. */
    public static List<DeferredHolder<Item, Item>> teaItems() {
        return List.copyOf(TEAS);
    }

    private FloraTeas() {
    }
}
