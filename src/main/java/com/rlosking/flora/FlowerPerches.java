package com.rlosking.flora;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every Flower Perch block: one empty perch (the crafting
 * output, the only one with an item form - the vanilla potted-flower
 * pattern) plus one planted perch per flower. VanillaBackport flowers are
 * registered only when VB is loaded; without it the catalogue (and the
 * "every flower bloomed" award target) simply shrinks.
 */
public final class FlowerPerches {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, KaleidoscopeFlora.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KaleidoscopeFlora.MOD_ID);

    public static final DeferredHolder<Block, EmptyFlowerPerchBlock> EMPTY_PERCH =
            BLOCKS.register("flower_perch", () -> new EmptyFlowerPerchBlock(perchProperties()));

    /**
     * The empty perch in item form: one item per wood species, all placing
     * the same block (the species rides on the blockstate). Filled inside
     * {@link #register(IEventBus)} because the pale-oak entry is gated on
     * VanillaBackport; enum order is the order the creative tabs show.
     */
    private static final Map<PerchWood, DeferredHolder<Item, Item>> EMPTY_PERCH_ITEMS =
            new EnumMap<>(PerchWood.class);

    /** The planted perch catalogue: id, flower item, petal colour, tall silhouette, VB-gated. */
    private record Flower(String id, ResourceLocation item, int color, boolean tall, boolean backport) {
    }

    private static final List<Flower> CATALOG = List.of(
            new Flower("dandelion", rl("minecraft:dandelion"), 0xF5C832, false, false),
            new Flower("poppy", rl("minecraft:poppy"), 0xC92A1E, false, false),
            new Flower("blue_orchid", rl("minecraft:blue_orchid"), 0x2EC4E0, false, false),
            new Flower("allium", rl("minecraft:allium"), 0xB57EDC, false, false),
            new Flower("azure_bluet", rl("minecraft:azure_bluet"), 0xEDEDE0, false, false),
            new Flower("red_tulip", rl("minecraft:red_tulip"), 0xD52B1E, false, false),
            new Flower("orange_tulip", rl("minecraft:orange_tulip"), 0xF07613, false, false),
            new Flower("white_tulip", rl("minecraft:white_tulip"), 0xEFEFE6, false, false),
            new Flower("pink_tulip", rl("minecraft:pink_tulip"), 0xECB5C2, false, false),
            new Flower("oxeye_daisy", rl("minecraft:oxeye_daisy"), 0xF2F2E6, false, false),
            new Flower("cornflower", rl("minecraft:cornflower"), 0x4666D8, false, false),
            new Flower("lily_of_the_valley", rl("minecraft:lily_of_the_valley"), 0xFFFFFF, false, false),
            new Flower("wither_rose", rl("minecraft:wither_rose"), 0x2B2B2B, false, false),
            new Flower("torchflower", rl("minecraft:torchflower"), 0xFF9B21, false, false),
            new Flower("sunflower", rl("minecraft:sunflower"), 0xF5C211, true, false),
            new Flower("lilac", rl("minecraft:lilac"), 0xB584C7, true, false),
            new Flower("rose_bush", rl("minecraft:rose_bush"), 0xC43C3C, true, false),
            new Flower("peony", rl("minecraft:peony"), 0xE8A9C9, true, false),
            new Flower("pitcher_plant", rl("minecraft:pitcher_plant"), 0xA54D38, true, false),
            new Flower("pink_petals", rl("minecraft:pink_petals"), 0xF2B5C6, false, false),
            new Flower("chorus_flower", rl("minecraft:chorus_flower"), 0xACA0C8, false, false),
            new Flower("spore_blossom", rl("minecraft:spore_blossom"), 0xE6A9C8, false, false),
            // VanillaBackport flowers: registered (and plantable) only with VB installed.
            new Flower("closed_eyeblossom", rl("minecraft:closed_eyeblossom"), 0xE8DCA0, false, true),
            new Flower("open_eyeblossom", rl("minecraft:open_eyeblossom"), 0xF2C94C, false, true),
            new Flower("wildflowers", rl("minecraft:wildflowers"), 0xE8E4C8, false, true),
            new Flower("cactus_flower", rl("minecraft:cactus_flower"), 0xE86A8A, false, true));

    /** Flower item id -> planted perch block, built once during registration. */
    private static final Map<ResourceLocation, DeferredHolder<Block, PlantedFlowerPerchBlock>> PERCH_BY_FLOWER =
            new LinkedHashMap<>();

    private static ResourceLocation rl(String path) {
        return ResourceLocation.parse(path);
    }

    /** Registers all perch blocks/items and the creative tab entries. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);

        boolean backport = net.neoforged.fml.ModList.get().isLoaded("vanillabackport");

        // One item per wood species, all nine placing the single empty-perch
        // block. Pale oak only exists with VanillaBackport, so its item is
        // skipped there - the blockstate variant stays but is unreachable.
        for (PerchWood wood : PerchWood.values()) {
            if (wood.backport() && !backport) {
                continue;
            }
            DeferredHolder<Item, Item> item = ITEMS.register(
                    "flower_perch_" + wood.getSerializedName(),
                    () -> new FlowerPerchItem(EMPTY_PERCH.get(), wood, new Item.Properties()));
            EMPTY_PERCH_ITEMS.put(wood, item);
        }

        for (Flower flower : CATALOG) {
            if (flower.backport() && !backport) {
                continue;
            }
            PERCH_BY_FLOWER.put(flower.item(),
                    BLOCKS.register("perch_" + flower.id(),
                            () -> new PlantedFlowerPerchBlock(perchProperties(), flower.item(), flower.color(), flower.tall())));
        }
    }

    /** The item form of an empty perch built from this wood. */
    public static DeferredHolder<Item, Item> emptyPerchItem(PerchWood wood) {
        DeferredHolder<Item, Item> item = EMPTY_PERCH_ITEMS.get(wood);
        return item != null ? item : EMPTY_PERCH_ITEMS.get(PerchWood.DEFAULT);
    }

    /** Every empty-perch item in this install, in enum order. */
    public static List<DeferredHolder<Item, Item>> emptyPerchItems() {
        return List.copyOf(EMPTY_PERCH_ITEMS.values());
    }

    /** The planted perch a stack of this flower plants into, or null. */
    public static Block perchFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        var holder = PERCH_BY_FLOWER.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return holder != null ? holder.get() : null;
    }

    /** Number of plantable flowers in this install (22, or 26 with VB). */
    public static int perchFlowerCount() {
        return PERCH_BY_FLOWER.size();
    }

    private static BlockBehaviour.Properties perchProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instabreak()
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .sound(SoundType.WOOD);
    }

    private FlowerPerches() {
    }
}
