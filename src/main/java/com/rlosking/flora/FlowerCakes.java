package com.rlosking.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The v0.3.4 flower cakes: one raw cake and the two things it cooks into.
 *
 * <p><b>One item, two processing routes.</b> A Raw Flower Cake is thrown, not
 * eaten; cooked in Cookery's wok it becomes a Toasted Flower Cake, steamed in
 * Cookery's steamer it becomes a Dew Flower Cake. Both finished cakes are
 * ordinary foods with this mod's effects attached. The recipes for all three
 * are plain JSON under {@code data/kaleidoscope_flora/recipe/} - no
 * generator, unlike the tea chain, because there is no table of 26 variants
 * to keep in step.</p>
 *
 * <p><b>Why the raw cake is a separate class.</b> It is the only one with
 * behaviour: right-click throws it and it pins whatever it hits
 * ({@link RawFlowerCakeItem}). The two finished cakes differ only in which
 * effects and food values they carry, so they share
 * {@link FlowerCakeItem} and are configured at registration.</p>
 *
 * <p><b>Stack sizes are not uniform, on purpose.</b> The raw cake stacks to 16
 * like a snowball, so a stack of them is a usable quiver. The finished cakes
 * are food and use ordinary food stacking - fifteen of them in a slot would
 * make the two effects effectively free.</p>
 *
 * <p><b>All three are also blocks (2026-09-28).</b> Sneak + right-click sets one
 * down, where up to five stack on the block and an empty hand takes one back -
 * the mooncake's behaviour, reused through {@link FlowerCakeBlock} and
 * {@link PlaceableCakeItem}. Three blocks are registered here, one per cake,
 * because each has its own model.</p>
 *
 * <p><b>⚠️ Language keys changed with that.</b> These are {@code BlockItem}s
 * now, and {@code BlockItem#getDescriptionId} returns the <em>block's</em> id,
 * so the game looks up {@code block.kaleidoscope_flora.<name>} - <b>not</b>
 * {@code item.kaleidoscope_flora.<name>}, which is what the entries used to be.
 * Both language files carry {@code block.*} keys; an {@code item.*} key alone
 * shows the raw translation key in game. (The mooncake has always been
 * {@code block.blossom_mooncake} for the same reason.)</p>
 */
public final class FlowerCakes {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, KaleidoscopeFlora.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, KaleidoscopeFlora.MOD_ID);

    /** Placed cake properties: the mooncake's tray settings, for the same reasons. */
    private static BlockBehaviour.Properties cakeBlock() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.5F)
                .sound(SoundType.CROP)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredHolder<Block, FlowerCakeBlock> RAW_BLOCK =
            BLOCKS.register("raw_flower_cake", () -> new FlowerCakeBlock(cakeBlock()));

    public static final DeferredHolder<Block, FlowerCakeBlock> TOASTED_BLOCK =
            BLOCKS.register("toasted_flower_cake", () -> new FlowerCakeBlock(cakeBlock()));

    public static final DeferredHolder<Block, FlowerCakeBlock> DEW_BLOCK =
            BLOCKS.register("dew_flower_cake", () -> new FlowerCakeBlock(cakeBlock()));

    /** The thrown cake: no hunger value, it is ammunition. */
    public static final DeferredHolder<Item, RawFlowerCakeItem> RAW =
            ITEMS.register("raw_flower_cake",
                    () -> new RawFlowerCakeItem(RAW_BLOCK.get(),
                            new Item.Properties().stacksTo(16)));

    /**
     * Wok route: strength, speed and a mining edge, with a doubled appetite,
     * then a sluggish comedown. (Strength replaced the old attack-speed role on
     * 2026-09-29 - see FlowerCakeItem.eatToasted.)
     *
     * <p>Food value is deliberately modest (4 hunger / 0.3 saturation) - this is
     * a buff carrier, not a meal, and the doubled hunger drain would be
     * self-defeating on something that also fed you well.</p>
     *
     * <p><b>{@code alwaysEdible()} is not optional.</b> Vanilla refuses to eat
     * anything while the hunger bar is full - {@code Player.canEat(false)} is
     * "hunger below 20" - so without this the cake could only be taken by a
     * player who was already hungry, which is the opposite of when a speed buff
     * is wanted. Reported in testing: "in survival I cannot eat the cakes at
     * all" (it worked in creative, where {@code abilities.invulnerable} makes
     * {@code canEat} return true unconditionally). Both finished cakes carry
     * the flag for the same reason.</p>
     */
    public static final DeferredHolder<Item, FlowerCakeItem> TOASTED =
            ITEMS.register("toasted_flower_cake",
                    () -> new FlowerCakeItem(TOASTED_BLOCK.get(), new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(4)
                                    .saturationModifier(0.3F)
                                    .alwaysEdible()
                                    .build())));

    /** Steamer route: an extra mid-air jump and a landing shockwave. */
    public static final DeferredHolder<Item, FlowerCakeItem> DEW =
            ITEMS.register("dew_flower_cake",
                    () -> new FlowerCakeItem(DEW_BLOCK.get(), new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(4)
                                    .saturationModifier(0.3F)
                                    .alwaysEdible()
                                    .build())));

    /** Every cake in creative-tab order: the chain reads raw, then its two routes. */
    private static final List<DeferredHolder<Item, ? extends Item>> CAKES =
            List.of(RAW, TOASTED, DEW);

    public static void register(IEventBus modBus) {
        // BLOCKS first: the item suppliers call <block>.get() and the block
        // registry event runs before the item one.
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Every flower cake in this install, in creative-tab order. */
    public static List<DeferredHolder<Item, ? extends Item>> cakeItems() {
        return new ArrayList<>(CAKES);
    }

    private FlowerCakes() {
    }
}
