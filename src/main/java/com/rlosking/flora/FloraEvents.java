package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IStockpot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

/**
 * All event-driven drink behaviours of Kaleidoscope Flora.
 *
 * <p>The drinks apply "marker" MobEffects (registered in {@link ModEffects}
 * without any logic); these handlers look for those markers and implement the
 * actual behaviour. Splitting it this way keeps every mechanic as plain
 * vanilla-or-NeoForge event code with zero custom packets or mixins.</p>
 *
 * <p><b>1.21.1 damage pipeline note:</b> NeoForge replaced the old Forge
 * damage events. {@code LivingIncomingDamageEvent} is the earliest, cancelable
 * stage (where the petal veil shatters projectiles and the kiss cloud
 * releases), {@code LivingDamageEvent.Pre} is the final stage before health is
 * applied (where reactions and the echo blink run), and amounts are read /
 * written through {@code getNewDamage()}/{@code setNewDamage(float)}.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraEvents {

    /** Lily-of-the-valley poison cloud cooldown per player (5 seconds). */
    private static final Map<UUID, Long> KISS_COOLDOWN = new HashMap<>();

    /**
     * Chorus echo grace: game time (inclusive) until which a saved player
     * cannot drop below one heart. Entry exists only during the 5 second
     * window after an echo rescue.
     */
    private static final Map<UUID, Long> ECHO_GRACE_UNTIL = new HashMap<>();

    /**
     * Last known server-side XZ per player. Player movement is client
     * authoritative: the server receives position packets (absMoveTo) that
     * never run {@code Entity#move}, so both {@code deltaMovement} and
     * {@code walkDist} stay at zero horizontally on the server. Effects that
     * need "is this player walking" on the server read the real per-tick
     * step measured from actual positions instead.
     */
    private static final Map<UUID, double[]> LAST_XZ = new HashMap<>();

    /** Per-player horizontal movement vector (dx, dz) of the last server tick. */
    private static final Map<UUID, double[]> MOVE_DELTA = new HashMap<>();

    /** Sniffer brush-dig progress per player: ticks held on dirt-family ground. */
    private static final Map<UUID, Integer> BRUSH_PROGRESS = new HashMap<>();

    /** The ground block each player is currently brushing (follows the crosshair). */
    private static final Map<UUID, BlockPos> BRUSH_TARGET = new HashMap<>();

    /**
     * Game time of the last brush event per player. While the use key is
     * held, the client re-fires {@code RightClickBlock} every 4 ticks and
     * keeps this fresh; once it goes stale the dig is discarded.
     */
    private static final Map<UUID, Long> BRUSH_LAST_EVENT = new HashMap<>();

    /** Petal veil costs 10 seconds of duration per blocked projectile. */
    private static final int PETAL_VEIL_BLOCK_COST_TICKS = 200;

    /** Lily poison cloud cooldown in ticks (5 seconds). */
    private static final int KISS_COOLDOWN_TICKS = 100;

    /**
     * Brushing a relic out of the ground takes this many ticks of holding
     * (2.4 s, advanced one tick per tick in {@link #onPlayerTick}).
     */
    private static final int BRUSH_DIG_TICKS = 48;

    /**
     * Silence threshold for the brush-dig session. The interaction event
     * renews the session every 4 ticks while the use key is held; if this
     * many ticks pass without renewal, the key was released and the dig
     * is discarded.
     */
    private static final int BRUSH_EVENT_TIMEOUT = 10;

    /**
     * After a find, the brush rests for this long (4 seconds) as the VANILLA
     * item cooldown - the familiar white sweep on the hotbar slot. Being a
     * real {@code ItemCooldowns} entry, it also stops the client from firing
     * the use key at all while it lasts, server and client stay in sync for
     * free, and other mods/datapacks can read it like any vanilla cooldown.
     */
    private static final int BRUSH_COOLDOWN_TICKS = 80;

    /** Length of the one-heart echo grace window (12 seconds). */
    private static final int ECHO_GRACE_TICKS = 240;

    /**
     * Lang keys of the three instant marker effects. Vanilla potion tooltips
     * only print a duration when it exceeds 20 ticks, so these would show up
     * with no time at all - the tooltip handler labels them "(Instant)"
     * instead.
     */
    private static final Set<String> INSTANT_EFFECT_KEYS = Set.of(
            "effect.kaleidoscope_flora.purge",
            "effect.kaleidoscope_flora.fadeaway",
            "effect.kaleidoscope_flora.wish");

    /**
     * Ancient seeds shipped by Immortalers Delight (千古乐事), dug up by the
     * sniffer soul. Soft dependency: resolved lazily so this mod runs with
     * or without that mod installed.
     */
    private static final ResourceLocation[] ANCIENT_SEED_IDS = {
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "alfalfa_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "gelpitaya_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "himekaido_seed"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "kwat_wheat_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "sextlotus_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "warped_laurel_seeds")};

    /**
     * The pale moss block only exists in 1.21.1 through VanillaBackport, so it
     * is resolved lazily (backport blocks register after this class loads);
     * resolves to air, a harmless never-matching block, when no backport is
     * installed.
     */
    private static Block paleMossBlock;

    /** Cookery's warmth effect holder, resolved lazily on first use. */
    private static Holder<MobEffect> warmthEffect;

    private FloraEvents() {
    }

    // ==================================================================
    // Stockpot: honey bottles
    // ==================================================================

    /**
     * Honey bottles and the stockpot have a container quirk: Cookery's
     * {@code getContainerItem} checks the FOOD component first, and vanilla
     * honey's FOOD has no {@code usingConvertsTo} (the drink-back-bottle logic
     * lives in HoneyBottleItem instead) - so it returns AIR and the pot never
     * returns a glass bottle on insertion, nor demands one on take-out.
     * Stripping FOOD from the copy that lands in the pot fixes both: the
     * lookup falls through to the item's crafting remainder (glass bottle),
     * and every container rule below becomes native Cookery behaviour.
     * <ul>
     *   <li>Insert honey bottle -> Cookery hands back exactly one glass
     *       bottle (we must NOT give one ourselves - that was the old
     *       double-bottle bug).</li>
     *   <li>Take it back out -> Cookery's containerIsMatch demands a glass
     *       bottle held in the main hand and consumes it, otherwise it shows
     *       its own "need container" action bar tip.</li>
     * </ul>
     * Slot placement: Cookery gives the bottle back BEFORE shrinking the input,
     * so with the honey still in hand the bottle lands in the next free slot
     * instead of the one it came from. Its {@code getItemToLivingEntity} puts
     * the bottle straight into the main hand when that slot is empty - so for a
     * single bottle we vacate the hand FIRST (Cookery refills it in place,
     * exactly like drinking a honey bottle); for a stack we keep vanilla
     * semantics (honey stays in hand, bottle goes to the inventory).
     * <p>
     * This handler deliberately runs on BOTH sides. If it ran server-only,
     * the client would still execute Cookery's own useItemOn with the bare
     * (FOOD-less) honey in hand - whose crafting-remainder lookup also yields
     * a glass bottle - and hand itself a ghost bottle as a prediction. The
     * server never knows about that ghost, the two inventories diverge, and
     * every insert/take-out cycle materialises another phantom bottle (the
     * infinite-bottle dupe). Cancelling the event on both sides and running
     * the identical logic on both sides keeps the two simulations in lock
     * step, so no ghost is ever created.
     */
    @SubscribeEvent
    public static void onRightClickStockpot(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof StockpotBlockEntity pot)) {
            return;
        }
        if (pot.hasLid() || pot.getStatus() != IStockpot.PUT_INGREDIENT) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(Items.HONEY_BOTTLE)) {
            return;
        }
        event.setCanceled(true);
        ItemStack bare = held.copy();
        bare.remove(DataComponents.FOOD);
        if (held.getCount() == 1) {
            // Vacate the hand slot first: Cookery's getItemToLivingEntity then
            // places the returned glass bottle directly into the empty main
            // hand - the same slot the honey bottle just left.
            held.setCount(0);
            if (!pot.addIngredient(level, event.getEntity(), bare)) {
                held.setCount(1); // pot refused it (e.g. all 9 slots full)
            }
        } else {
            if (pot.addIngredient(level, event.getEntity(), bare)) {
                held.shrink(1); // remaining honey stays put, bottle to inventory
            }
        }
    }

    // ==================================================================
    // Stockpot: teapot scooping
    // ==================================================================

    /**
     * Scooping the finished pot into Cookery's teapot.
     *
     * <p>The teapot was built for a 12-cup brew; the stockpot caps at 9.
     * A full pot (9 cups) fits inside one teapot with room to spare, so a
     * player who wants the vanilla "carry the pot, pour cup by cup"
     * ritual can do it with our flower drinks too:</p>
     * <ol>
     *   <li>Brew a drink in the stockpot (our recipes yield 9 cups).</li>
     *   <li>Right-click the FINISHED pot with an EMPTY teapot.</li>
     *   <li>The whole pot is transferred into the teapot item as Cookery's
     *       native "Result" block-entity data (Status 2 + Result stack).
     *       From here on everything is vanilla Cookery behaviour: the
     *       teapot tooltip shows the contents, the durability-style bar
     *       shows 9/12, and pouring into empty cups / stacked teacup
     *       blocks works exactly like brewing in the teapot itself.</li>
     * </ol>
     *
     * <p><b>Rules (author decision, 2026-09-01):</b> only drinks whose
     * item is a Cookery {@code TeacupItem} may be scooped (a teapot full
     * of soup could never be poured into cups), and the teapot must be
     * completely empty - no water, no leftover tea.</p>
     *
     * <p><b>Reflection note:</b> the pot's public API can read
     * {@code getResult()} and {@code getTakeoutCount()} but has no
     * setter, so draining the pot touches Cookery's private fields
     * {@code takeoutCount}, {@code result}, {@code recipeId},
     * {@code soupBaseId}, {@code status}, {@code inputs} and
     * {@code currentTick} directly - mirroring exactly what Cookery's own
     * {@code takeOutProduct} does when its takeout count hits zero. Field
     * names are verified against the decompiled 1.4.1 jar.</p>
     */
    @SubscribeEvent
    public static void onRightClickStockpotWithTeapot(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (!(level.getBlockEntity(event.getPos()) instanceof StockpotBlockEntity pot)) {
            return;
        }
        if (pot.hasLid() || pot.getStatus() != IStockpot.FINISHED) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(ModItems.TEAPOT)) {
            return;
        }
        event.setCanceled(true);
        if (level.isClientSide()) {
            return; // prediction-friendly: server is authoritative
        }

        // Rule 2: the teapot must be completely empty - no fluid and no
        // leftover tea from a previous scoop.
        CustomData data = held.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null && !data.copyTag().isEmpty()) {
            event.getEntity().displayClientMessage(
                    Component.translatable("tip.kaleidoscope_flora.teapot_scoop.not_empty"), true);
            return;
        }

        // Rule 1: only teacup drinks may be scooped into a teapot.
        ItemStack potResult = pot.getResult();
        if (potResult.isEmpty() || !(potResult.getItem() instanceof TeacupItem)) {
            event.getEntity().displayClientMessage(
                    Component.translatable("tip.kaleidoscope_flora.teapot_scoop.not_drink"), true);
            return;
        }

        // Transfer the whole pot into the teapot as Cookery-native data:
        // Status 2 ("tea ready") + a Result stack of the drink. The count
        // is capped at 12 like a real teapot brew.
        CompoundTag tag = new CompoundTag();
        tag.putInt("Status", 2);
        ItemStack teaStack = potResult.copyWithCount(Math.min(potResult.getCount(), TEAPOT_MAX_CUPS));
        tag.put("Result", teaStack.saveOptional(level.registryAccess()));
        BlockItem.setBlockEntityData(held, ModBlocks.TEAPOT_BE.get(), tag);

        // Drain the pot exactly like Cookery's takeOutProduct on its last
        // cup: status back to empty, inputs cleared, everything reset.
        drainStockpot(pot, level);

        level.playSound(null, event.getPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /** Cookery's teapot capacity, {@code TeapotRecipe.OUTPUT_COUNT}. */
    private static final int TEAPOT_MAX_CUPS = 12;

    /**
     * Empties a finished stockpot after a teapot scoop, resetting every
     * piece of state exactly like Cookery's own "last cup taken out"
     * branch in {@code takeOutProduct}.
     */
    private static void drainStockpot(StockpotBlockEntity pot, Level level) {
        try {
            Class<?> cls = pot.getClass();
            findField(cls, "takeoutCount").set(pot, 0);
            findField(cls, "result").set(pot, ItemStack.EMPTY);
            findField(cls, "recipeId").set(pot, StockpotRecipeSerializer.EMPTY_ID);
            findField(cls, "soupBaseId").set(pot, ModSoupBases.WATER);
            findField(cls, "status").set(pot, 0);
            Field inputsField = findField(cls, "inputs");
            if (inputsField.get(pot) instanceof NonNullList<?> inputs) {
                inputs.clear();
            }
            findField(cls, "currentTick").set(pot, -1);
            findField(cls, "renderEntity").set(pot, null);
            pot.refresh();
        } catch (ReflectiveOperationException e) {
            KaleidoscopeFlora.LOGGER.error("Failed to drain stockpot after teapot scoop", e);
        }
    }

    /** Cached field lookup helper for Cookery's private stockpot fields. */
    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            // StockpotBlockEntity extends BaseBlockEntity - search parents
            Class<?> parent = cls.getSuperclass();
            if (parent != null) {
                return findField(parent, name);
            }
            throw e;
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // Chorus flower "Echo of the End": a fatal blow never lands. The echo
        // blinks you to a standable block nearby, grants 12 seconds of Fire
        // Resistance, Haste V and Jump Boost V for the escape, and holds you
        // at exactly one heart for 12 seconds (ECHO_GRACE window). The drink
        // pays for the rescue with itself - one cheat of death per cup.
        if (target.hasEffect(ModEffects.ECHO)
                && event.getAmount() >= target.getHealth() + target.getAbsorptionAmount()
                && target.level() instanceof ServerLevel server) {
            event.setCanceled(true);
            performEchoRescue(server, target);
            return;
        }

        // Lilac "Spring Waltz" (Cookery warmth): fully immune to freezing
        // damage - powder snow and freeze ticks included.
        if (event.getSource().is(DamageTypeTags.IS_FREEZING) && hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        // The same warmth at the other extreme: no fire, lava or hot floor
        // can scald the drinker either - the kettle sits above the flame,
        // never in it.
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        // Pink petals "Hanami Tale": every projectile that would hit the
        // drinker is shattered into petals instead. Each block spends 10
        // seconds of the veil's duration - beauty consumed to protect you.
        // Melee is unaffected.
        if (target.hasEffect(ModEffects.PETAL_VEIL)
                && event.getSource().getDirectEntity() instanceof Projectile projectile) {
            event.setCanceled(true);
            projectile.discard();
            if (target instanceof ServerPlayer veiled) {
                FloraAdvancements.petalVeilBlock(veiled);
            }
            shortenEffect(target, ModEffects.PETAL_VEIL, PETAL_VEIL_BLOCK_COST_TICKS);
            if (target.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.5, 0.8, 0.5, 0.1);
                server.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return;
        }

        // Lily of the valley "May Kiss": being hurt releases a poison cloud
        // around the drinker (5 second cooldown).
        if (target.hasEffect(ModEffects.KISS)) {
            long now = target.level().getGameTime();
            Long last = KISS_COOLDOWN.get(target.getUUID());
            if (last == null || now - last >= KISS_COOLDOWN_TICKS) {
                KISS_COOLDOWN.put(target.getUUID(), now);
                for (LivingEntity nearby : target.level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(3.0))) {
                    if (nearby != target) {
                        nearby.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    }
                }
                if (target.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.EFFECT,
                            target.getX(), target.getY() + 1.0, target.getZ(), 40, 1.0, 0.8, 1.0, 0.05);
                }
            }
        }
    }

    // ==================================================================
    // Final damage stage: vampirism, ignition, thorns, echo blink
    // ==================================================================

    /** One shared hook for everything that reacts to real damage being dealt. */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        float damage = event.getNewDamage();
        if (damage <= 0) {
            return;
        }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity a ? a : null;

        // Red tulip "Crimson Heartbeat": 20% of dealt damage returns as
        // health. No particles by design: the client HUD reacts to any
        // health gain with the vanilla white-bordered heart flash - the
        // same animation natural regeneration shows after eating - so the
        // lifesteal healing announces itself exactly the way vanilla food
        // does (nothing shows at full health, matching vanilla behaviour).
        if (attacker != null && attacker.hasEffect(ModEffects.VAMPIRIC)) {
            attacker.heal(damage * 0.20f);
        }

        // Allium "Fire Waltz": the drinker's melee hits set targets ablaze.
        if (attacker != null && attacker.hasEffect(ModEffects.FIREBRAND)) {
            victim.igniteForSeconds(6);
        }

        // Rose bush "Tender Thorns": the thorn answers in kind - the attacker
        // takes back a share of the very blow it landed, rolled per hit:
        // 70% half, 25% full price, 5% double. Plus weakness either way.
        if (victim.hasEffect(ModEffects.THORNS) && attacker != null && attacker != victim) {
            float roll = victim.getRandom().nextFloat();
            float share = roll < 0.05f ? 2.0f : roll < 0.30f ? 1.0f : 0.5f;
            attacker.hurt(victim.damageSources().thorns(victim), damage * share);
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }

        // Chorus echo grace (set by performEchoRescue): for 12 seconds after
        // the rescue the saved one cannot drop below one heart. Damage is
        // clamped, not negated - it still hurts, it just cannot finish the
        // job while the escape window lasts.
        Long graceUntil = ECHO_GRACE_UNTIL.get(victim.getUUID());
        if (graceUntil != null) {
            if (victim.level().getGameTime() > graceUntil) {
                ECHO_GRACE_UNTIL.remove(victim.getUUID());
            } else {
                float floor = Math.min(2.0f, victim.getMaxHealth());
                if (event.getNewDamage() > victim.getHealth() - floor) {
                    event.setNewDamage(Math.max(0.0f, victim.getHealth() - floor));
                }
            }
        }
    }

    /**
     * The chorus echo rescue: consumes the effect, blinks the victim onto a
     * random standable block within sight range, sets them to exactly one
     * heart, opens the 12 second one-heart grace window, and hands them
     * Fire Resistance + Haste V + Jump Boost V for 12 seconds to get away
     * with.
     */
    private static void performEchoRescue(ServerLevel server, LivingEntity target) {
        target.removeEffect(ModEffects.ECHO);
        ECHO_GRACE_UNTIL.put(target.getUUID(), server.getGameTime() + ECHO_GRACE_TICKS);

        // "大难不死" (Cheated Death): the echo answered a killing blow.
        if (target instanceof ServerPlayer rescued) {
            FloraAdvancements.award(rescued, FloraAdvancements.EVENT_CHEATED_DEATH);
        }

        double fromX = target.getX();
        double fromY = target.getY();
        double fromZ = target.getZ();
        server.sendParticles(ParticleTypes.PORTAL,
                fromX, fromY + 1.0, fromZ, 40, 0.5, 1.0, 0.5, 0.3);

        BlockPos spot = findStandableSpot(server, target, target.getRandom());
        if (spot != null) {
            double x = spot.getX() + 0.5;
            double y = spot.getY();
            double z = spot.getZ() + 0.5;
            target.teleportTo(x, y, z);
            server.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 40, 0.5, 1.0, 0.5, 0.3);
            server.playSound(null, x, y, z,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            server.playSound(null, fromX, fromY, fromZ,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        target.setHealth(Math.min(2.0f, target.getMaxHealth())); // exactly one heart
        target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ECHO_GRACE_TICKS, 0)); // no scalding escape
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ECHO_GRACE_TICKS, 4)); // Haste V
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, ECHO_GRACE_TICKS, 4));      // Jump V
        // Haste only quickens digging in vanilla - an escape needs legs too.
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ECHO_GRACE_TICKS, 4)); // Speed V
    }

    /**
     * A random standable spot within 12 blocks horizontally / 4-6 vertically:
     * solid top-face ground below, two free collision blocks above it, no
     * fluid. Returns null when nothing qualifying is found in 32 attempts.
     */
    private static BlockPos findStandableSpot(ServerLevel level, LivingEntity entity, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos pos = entity.blockPosition()
                    .offset(random.nextInt(25) - 12, random.nextInt(11) - 4, random.nextInt(25) - 12);
            BlockPos ground = pos.below();
            if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
                continue;
            }
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                continue;
            }
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
                continue;
            }
            return pos;
        }
        return null;
    }

    // ==================================================================
    // Cornflower "The Prussian Leap": fall damage immunity
    // ==================================================================

    /**
     * Cancelling the whole fall (instead of a zero damage multiplier) keeps
     * the entity logic intact. Vanilla only plays the landing thump when
     * damage actually lands, so a feather-soft landing would be silent - the
     * sound is reproduced here exactly the way {@code playBlockFallSound}
     * does it.
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.FEATHERFALL)) {
            return;
        }
        event.setCanceled(true);
        if (entity.isSilent() || event.getDistance() <= 1.5f) {
            return;
        }
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.2F, entity.getZ());
        BlockState state = entity.level().getBlockState(below);
        if (!state.isAir()) {
            SoundType soundType = state.getSoundType(entity.level(), below, entity);
            entity.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5F, soundType.getPitch() * 0.75F);
        }
    }

    // ==================================================================
    // Per-tick effects: ambient orbits and the thaw
    // ==================================================================

    /**
     * Runs for every living entity, server side only:
     * <ul>
     *   <li>Lilac "Spring Waltz" (Cookery warmth): freezing ticks melt away
     *       every tick, so powder snow can neither slow nor frostbite the
     *       drinker (belt to the damage-type cancellation in
     *       {@link #onIncomingDamage}).</li>
     *   <li>Pink petals "Hanami Tale": two cherry petals orbit the drinker
     *       for as long as the veil lasts - the beauty is visible even when
     *       nothing is being blocked.</li>
     *   <li>Peony "Coronation" (luck): golden motes circle the lucky one.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel server)) {
            return;
        }
        if (hasWarmth(entity)) {
            // Spring Waltz: the thaw AND the flame-out - freezing ticks melt
            // away every tick, and any fire the drinker waded through is
            // snuffed, so even lava leaves nothing but the swim.
            if (entity.getTicksFrozen() > 0) {
                entity.setTicksFrozen(0);
            }
            if (entity.isOnFire()) {
                entity.setRemainingFireTicks(0);
            }
        }
        if (server.getGameTime() % 4 == 0) {
            if (entity.hasEffect(ModEffects.PETAL_VEIL)) {
                orbitParticles(server, entity, ParticleTypes.CHERRY_LEAVES);
            }
        }
        // Peony "Coronation" (luck): golden dust that fades in and out.
        if (entity.hasEffect(MobEffects.LUCK)) {
            coronationMotes(server, entity);
        } else {
            LUCK_START.remove(entity.getUUID());
        }
        // Orange tulip "Autumn Serenade": loose drops drift to the harvester.
        if (entity.hasEffect(ModEffects.HARVEST)) {
            attractDrops(server, entity);
        }
    }

    /** Two particles circling the entity at shoulder height, rotating over time. */
    private static void orbitParticles(ServerLevel server, LivingEntity entity, ParticleOptions particle) {
        long time = server.getGameTime();
        for (int i = 0; i < 2; i++) {
            double angle = time / 10.0 + i * Math.PI;
            double x = entity.getX() + Math.cos(angle) * 0.9;
            double z = entity.getZ() + Math.sin(angle) * 0.9;
            double y = entity.getY() + 1.0 + Math.sin(time / 12.0 + i * 2.0) * 0.2;
            server.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** When each lucky one's luck began, for the fade-in ramp. */
    private static final Map<UUID, Long> LUCK_START = new HashMap<>();

    /**
     * Golden motes for the coronation: a gold-dust orbit whose density ramps
     * up over the first two seconds and thins out over the last three, so
     * the blessing appears and dissolves instead of popping in and out.
     * Each dust mote also fades on its own over its lifetime, which makes
     * the orbit read as a soft golden haze rather than a hard ring.
     */
    private static void coronationMotes(ServerLevel server, LivingEntity entity) {
        long time = server.getGameTime();
        UUID uuid = entity.getUUID();
        Long started = LUCK_START.get(uuid);
        if (started == null) {
            started = time;
            LUCK_START.put(uuid, started);
        }
        MobEffectInstance luck = entity.getEffect(MobEffects.LUCK);
        if (luck == null) {
            return;
        }
        double fadeIn = Math.min(1.0, (time - started) / 40.0);
        double fadeOut = Math.min(1.0, luck.getDuration() / 60.0);
        double density = fadeIn * fadeOut;
        DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.0F), 0.9F);
        for (int i = 0; i < 2; i++) {
            if (entity.getRandom().nextDouble() > density * 0.5) {
                continue;
            }
            double angle = time / 10.0 + i * Math.PI;
            double x = entity.getX() + Math.cos(angle) * 0.9;
            double z = entity.getZ() + Math.sin(angle) * 0.9;
            double y = entity.getY() + 1.0 + Math.sin(time / 12.0 + i * 2.0) * 0.2;
            server.sendParticles(gold, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /**
     * Autumn Serenade magnet: item entities within 5 blocks drift toward
     * the harvester - the falling leaves drawn to the basket. The pull grows
     * as the drop closes in, so items accelerate instead of hovering.
     */
    private static void attractDrops(ServerLevel server, LivingEntity entity) {
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class,
                entity.getBoundingBox().inflate(5.0));
        if (items.isEmpty()) {
            return;
        }
        Vec3 center = new Vec3(entity.getX(), entity.getY() + 0.6, entity.getZ());
        for (ItemEntity item : items) {
            Vec3 pull = center.subtract(item.position());
            double distance = pull.length();
            if (distance < 0.3 || distance > 5.0) {
                continue;
            }
            double speed = 0.08 + 0.12 * (1.0 - distance / 5.0);
            item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(pull.normalize().scale(speed)));
        }
    }

    // ==================================================================
    // Blue orchid "First Bloom": food restores 50% more
    // ==================================================================

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();

        // Advancement hooks: every finished flora drink counts toward the
        // collection line and may fire one of the three drinking memes.
        if (entity instanceof ServerPlayer player && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.drinkFinished(player, event.getItem());
        }

        if (!entity.hasEffect(ModEffects.TASTEBLOOM) || !(entity instanceof Player eater)) {
            return;
        }
        FoodProperties food = event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
        if (food != null) {
            eater.getFoodData().eat(Math.round(food.nutrition() * 0.5f), food.saturation() * 0.5f);
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        // Hunger as it was before the first sip: by the time Finish fires,
        // the drink's food value has already restocked the bar, so "drank on
        // an empty stomach" can only be judged at the start.
        if (event.getEntity() instanceof ServerPlayer player
                && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.sipStarted(player);
        }
    }

    // ==================================================================
    // Pitcher plant "The Voracious Urn": kills count as +1 looting level
    // ==================================================================

    /**
     * The urn's greed has two mouths: every stack the player's kill dropped
     * has a 50% chance to grow by one (the binomial bonus vanilla Looting I
     * adds to common drops - NeoForge 1.21.1 has no looting-level event, so
     * the statistical outcome is reproduced instead), and then the whole loot
     * sometimes comes again: 70% of kills keep it at face value (x1), 30%
     * double it (x2), stacked on top of the looting bonus.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.isRecentlyHit()
                || !(event.getSource().getEntity() instanceof Player player)
                || !player.hasEffect(ModEffects.DIGESTION)) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.getMaxStackSize() > 1 && player.getRandom().nextInt(2) == 0) {
                stack.grow(1);
            }
        }
        // The second mouth: duplicate the (already looting-grown) loot 30% of
        // the time; the other 70% the urn keeps its appetite to itself.
        int multiplier = player.getRandom().nextFloat() < 0.30f ? 2 : 1;
        List<ItemEntity> drops = List.copyOf(event.getDrops());
        for (int i = 1; i < multiplier; i++) {
            for (ItemEntity drop : drops) {
                ItemEntity copy = new ItemEntity(event.getEntity().level(),
                        drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                copy.setDefaultPickUpDelay();
                event.getDrops().add(copy);
            }
        }
        // "贪心不足" (Greed Without End): three doubled drops in one urn.
        if (multiplier > 1 && player instanceof ServerPlayer urnBearer) {
            FloraAdvancements.urnDoubleDrop(urnBearer);
        }
    }

    // ==================================================================
    // Orange tulip "Autumn Serenade": mature crops drop x2-x4
    // ==================================================================

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }
        if (!(event.getBreaker() instanceof Player breaker) || !breaker.hasEffect(ModEffects.HARVEST)) {
            return;
        }
        // One random multiplier of 2, 3 or 4 per harvest; spawn the extra
        // copies of every drop entity (count-1 more copies of each stack).
        int multiplier = 2 + breaker.getRandom().nextInt(3);
        List<ItemEntity> drops = List.copyOf(event.getDrops());
        for (int i = 1; i < multiplier; i++) {
            for (ItemEntity drop : drops) {
                ItemEntity copy = new ItemEntity(event.getLevel(),
                        drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                copy.setDefaultPickUpDelay();
                event.getLevel().addFreshEntity(copy);
            }
        }
    }

    // ==================================================================
    // White tulip "Absolution": harmful effects cannot land
    // ==================================================================

    /**
     * {@code MobEffectEvent.Applicable} is fired before an effect is actually
     * applied, so vetoing here stops harmful effects from ever taking hold -
     * already-running ones are untouched on purpose (that cure is "When the
     * Wind Rises").
     */
    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().hasEffect(ModEffects.ABSOLVE)
                && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ==================================================================
    // Torchflower "Breath of the Ancients": brush-digging for relics
    // ==================================================================

    /**
     * While the sniffer soul is active, HOLDING a brush against diggable
     * ground (dirt family, mosses, mud, sand, red sand, gravel - but NEVER
     * vanilla suspicious sand/gravel, those stay pure vanilla archaeology)
     * digs for ancient relics - no sneaking required. A single click does
     * nothing.
     *
     * <p><b>Why the event is cancelled on BOTH logical sides:</b> vanilla
     * {@code BrushItem#useOn} returns CONSUME and calls
     * {@code player.startUsingItem(...)} for <i>any</i> aimed block (verified
     * in the bytecode - it is not limited to suspicious sand). Once the
     * client thinks the brush is being used, {@code isUsingItem()} goes true
     * and {@code Minecraft#handleKeybinds} stops re-firing the use key -
     * so the server receives exactly ONE interaction packet per press and a
     * hold-based dig can never accumulate. Cancelling the client-side
     * prediction event keeps the use-key loop alive: one
     * {@code ServerboundUseItemOnPacket} reaches the server every 4 ticks
     * for as long as the key is held, and each one renews the dig session
     * that {@link #onPlayerTick} advances tick by tick.</p>
     *
     * <p>Every find spends brush durability, a 4 second VANILLA item cooldown
     * (the white sweep on the hotbar slot) and buff duration according to
     * the find's value (the drink starts with 5 minutes). Loot table
     * (weights sum to 120): torchflower seeds 30 / pitcher pod 12 / coal 16
     * / iron nugget 14 / gold nugget 8 / bone 8 / bone meal 12 / pottery
     * sherd 10 - region-locked, see {@link SherdPool} / Immortalers Delight
     * ancient seed 8 / sniffer egg 2.</p>
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !player.hasEffect(ModEffects.SNIFFER_SOUL)
                || !event.getItemStack().is(Items.BRUSH)
                || !isSnifferGround(player.level().getBlockState(event.getPos()))) {
            return;
        }
        event.setCanceled(true);

        if (player.level().isClientSide()) {
            // A local arm sweep every 4 ticks so holding the key visibly
            // "brushes" the ground instead of looking dead.
            player.swing(InteractionHand.MAIN_HAND);
            return;
        }

        ServerLevel server = (ServerLevel) player.level();
        UUID uuid = player.getUUID();
        long now = server.getGameTime();

        // Rest after a find: the vanilla item cooldown on the brush keeps the
        // client from re-firing the use key anyway, so no new session can
        // start while it lasts.
        if (player.getCooldowns().isOnCooldown(Items.BRUSH)) {
            return;
        }

        // Renew the session: a fresh dig when idle, otherwise keep the
        // accumulated progress and follow wherever the crosshair sweeps.
        // A brand-new session plays the digging rustle RIGHT AWAY, the way
        // the vanilla sniffer does when it starts digging - playing it only
        // at completion made the dig sound arrive after the item had already
        // popped out, which read as the sound lagging one dig behind.
        boolean freshDig = !BRUSH_PROGRESS.containsKey(uuid);
        BRUSH_TARGET.put(uuid, event.getPos());
        BRUSH_LAST_EVENT.put(uuid, now);
        BRUSH_PROGRESS.putIfAbsent(uuid, 0);
        if (freshDig) {
            server.playSound(null, event.getPos(), SoundEvents.SNIFFER_DIGGING,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    /**
     * Advances an active brush-dig session once per tick and finishes it
     * when the hold time is reached. The session dies the moment the use
     * key is released: {@code RightClickBlock} stops renewing it, and after
     * {@link #BRUSH_EVENT_TIMEOUT} ticks of silence the leftovers are
     * discarded.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        UUID uuid = player.getUUID();

        // Measure the real per-tick step for the server-side effects (see
        // LAST_XZ): must run before any early return below.
        double[] last = LAST_XZ.get(uuid);
        double x = player.getX();
        double z = player.getZ();
        MOVE_DELTA.put(uuid, last == null ? new double[]{0.0, 0.0} : new double[]{x - last[0], z - last[1]});
        LAST_XZ.put(uuid, new double[]{x, z});

        if (!BRUSH_TARGET.containsKey(uuid)) {
            return;
        }
        ServerLevel server = (ServerLevel) player.level();
        long now = server.getGameTime();

        // Released the use key: too long since the last interaction event.
        if (now - BRUSH_LAST_EVENT.getOrDefault(uuid, now) > BRUSH_EVENT_TIMEOUT) {
            clearBrushSession(uuid);
            return;
        }
        // Swapped the brush away or lost the buff mid-dig.
        if (!player.hasEffect(ModEffects.SNIFFER_SOUL)
                || !player.getMainHandItem().is(Items.BRUSH)) {
            clearBrushSession(uuid);
            return;
        }
        // Rest after a find: refuse to dig while the vanilla brush cooldown lasts.
        if (player.getCooldowns().isOnCooldown(Items.BRUSH)) {
            clearBrushSession(uuid);
            return;
        }

        BlockPos pos = BRUSH_TARGET.get(uuid);
        int progress = BRUSH_PROGRESS.getOrDefault(uuid, 0) + 1;
        if (progress < BRUSH_DIG_TICKS) {
            BRUSH_PROGRESS.put(uuid, progress);
            // Work-in-progress feedback every 6 ticks: soil crumbs + hiss.
            if (progress % 6 == 0) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, server.getBlockState(pos)),
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.3, 0.05, 0.3, 0.0);
                server.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.4f, 1.0f);
            }
            return;
        }

        clearBrushSession(uuid);
        // The vanilla cooldown: white sweep on the hotbar slot, use key
        // blocked client-side, sync handled by ServerItemCooldowns.
        player.getCooldowns().addCooldown(Items.BRUSH, BRUSH_COOLDOWN_TICKS);
        player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        server.playSound(null, pos, SoundEvents.SNIFFER_DROP_SEED, SoundSource.BLOCKS, 1.0f, 1.0f);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.5, 0.3, 0.5, 0.1);

        SnifferDig loot = rollSnifferLoot(server, pos, player.getRandom());
        if (!loot.stack().isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(server,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, loot.stack());
            itemEntity.setDefaultPickUpDelay();
            server.addFreshEntity(itemEntity);
        }
        shortenEffect(player, ModEffects.SNIFFER_SOUL, loot.costTicks());
    }

    /** Discards every trace of a player's brush-dig session. */
    private static void clearBrushSession(UUID uuid) {
        BRUSH_PROGRESS.remove(uuid);
        BRUSH_TARGET.remove(uuid);
        BRUSH_LAST_EVENT.remove(uuid);
    }

    // ==================================================================
    // Pink tulip "Rosy Stride": land-style view bob while on the water
    // ==================================================================

    /**
     * The petal film holds the drinker's feet ABOVE the fluid, so
     * {@code Entity#move()} reports no block collision and
     * {@code Player#aiStep}'s bob branch - which requires
     * {@code onGround() && !isSwimming()} - zeroes out. Walking on water
     * therefore had no view bob at all.
     *
     * <p>This runs in {@code PlayerTickEvent.Post}, i.e. AFTER that update,
     * and re-applies the exact vanilla formula
     * ({@code bob = oBob + (min(0.1, horizSpeed) - oBob) * 0.4}) - {@code oBob}
     * was already stored correctly by {@code aiStep}, so the camera sways
     * on the film precisely like it does on land: same amplitude, same
     * frequency, decaying when standing still or mid-jump.</p>
     *
     * <p>Must run on BOTH sides - the bob fields are only ever read for the
     * client player, but the event has no side filter anyway, and on land
     * the recomputation is a no-op (identical numbers) so nothing fights.</p>
     */
    @SubscribeEvent
    public static void onPlayerTickPetalBob(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.PETALWALK)
                || player.isSwimming()
                || player.isDeadOrDying()) {
            return; // diving deep or dead: no bob, like vanilla
        }
        if (player.onGround() || !isOnPetalFilm(player)) {
            return; // real ground handles it natively; mid-air/jumping decays like vanilla
        }
        float stride = Math.min(0.1F, (float) player.getDeltaMovement().horizontalDistance());
        player.bob = player.oBob + (stride - player.oBob) * 0.4F;
    }

    /**
     * True when the player's feet rest on the petal film riding a water
     * surface - the same window {@code PetalWalkEffect} pins them inside
     * (a hair above the surface, with the gravity-sag tolerance below it).
     * Package-private: {@link FloraAdvancements} reuses it for the water
     * crossing award.
     */
    static boolean isOnPetalFilm(Player player) {
        BlockPos support = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        FluidState fluid = player.level().getFluidState(support);
        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }
        // Same topmost-layer rule as PetalWalkEffect: no film (and no bob)
        // while inside the body of water, only on its surface.
        if (player.level().getFluidState(support.above()).is(FluidTags.WATER)) {
            return false;
        }
        double surface = support.getY() + fluid.getHeight(player.level(), support);
        double feet = player.getY();
        return feet >= surface - 0.05 && feet <= surface + 0.15;
    }

    // ==================================================================
    // Advancements: per-tick bookkeeping and the lullaby dawn
    // ==================================================================

    /**
     * Server-side advancement bookkeeping for every player, once per tick.
     * Delegates to {@link FloraAdvancements#tick}: clears meme counters of
     * lapsed effects, accumulates the Rosy Stride water crossing, and once
     * per second checks the hidden collector award.
     */
    @SubscribeEvent
    public static void onPlayerTickAdvancements(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        FloraAdvancements.tick(player);
    }

    /**
     * "一晌贪欢" (A Moment of Stolen Joy): the lullaby drinker falls asleep
     * drowsy and wakes at dawn with the effect still running - the night
     * skip advances the clock but never the buff's duration, so the nap
     * really did cost the dreamer nothing. {@code PlayerWakeUpEvent} fires
     * on both logical sides; only the server awards. Waking at dawn means
     * the game time landed in the first ticks of a fresh day - a wake-up
     * in the middle of the night (interrupted by a monster, say) is
     * exactly the "stolen" joy this award refuses.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModEffects.DROWSY) && player.level().getDayTime() % 24000L < 2400L) {
            FloraAdvancements.award(player, FloraAdvancements.EVENT_LULLABY_DAWN);
        }
    }

    /**
     * Everything the sniffer soul can brush relics out of: the dirt family,
     * mosses, mud, sand, red sand and gravel. Vanilla suspicious sand and
     * suspicious gravel are deliberately excluded BEFORE the {@code #sand}
     * tag check - that vanilla tag DOES contain suspicious sand, and without
     * this guard the dig would hijack vanilla archaeology sites. Brushing a
     * suspicious block must resolve through the vanilla loot path, untouched.
     */
    private static boolean isSnifferGround(BlockState state) {
        if (state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL)) {
            return false;
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
            return true;
        }
        if (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)) {
            return true;
        }
        if (paleMossBlock == null) {
            paleMossBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("pale_moss_block"));
        }
        return state.is(paleMossBlock);
    }

    /** One dig result: what you find and how much buff duration it costs. */
    private record SnifferDig(ItemStack stack, int costTicks) {
    }

    /**
     * Vanilla pottery sherd sets, mirroring where vanilla archaeology finds
     * them: desert pyramids and wells, warm and cold ocean ruins, trail
     * ruins. The brush dig borrows the same regional logic - the biome you
     * dig in decides which ancient culture left its sherds behind, so
     * collecting all twenty sherds means travelling, exactly like hunting
     * vanilla sites.
     */
    private enum SherdPool {
        DESERT(Items.ARCHER_POTTERY_SHERD, Items.MINER_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD,
                Items.SKULL_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD),
        WARM_OCEAN(Items.ANGLER_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.SNORT_POTTERY_SHERD),
        COLD(Items.BLADE_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.MOURNER_POTTERY_SHERD,
                Items.PLENTY_POTTERY_SHERD),
        TEMPERATE(Items.BURN_POTTERY_SHERD, Items.DANGER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD,
                Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD,
                Items.SHEAF_POTTERY_SHERD);

        final Item[] sherds;

        SherdPool(Item... sherds) {
            this.sherds = sherds;
        }
    }

    /**
     * Maps the dig position to its regional sherd set. Deserts and badlands
     * (hot, dry - no precipitation) hold the pyramid and well sherds; ocean
     * and beach ground holds ruin sherds, warm or cold by biome temperature;
     * snowy ground shares the cold ruin set; everything else gets the trail
     * ruins set. Non-overworld dimensions have no vanilla ruins, so they
     * default to the trail ruins set.
     */
    private static SherdPool sherdPoolAt(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return SherdPool.TEMPERATE;
        }
        Holder<Biome> biome = level.getBiome(pos);
        boolean cold = biome.value().getBaseTemperature() < 0.2F;
        if (biome.is(BiomeTags.IS_BADLANDS)
                || !biome.value().hasPrecipitation()) {
            return SherdPool.DESERT;
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) {
            return cold ? SherdPool.COLD : SherdPool.WARM_OCEAN;
        }
        return cold ? SherdPool.COLD : SherdPool.TEMPERATE;
    }

    /**
     * Weighted ancient-relic table for the brush dig; costs are 1/20 of a
     * second. Pottery sherds are region-locked: the dig position picks one
     * of the {@link SherdPool} sets, then the roll picks a sherd from it.
     */
    private static SnifferDig rollSnifferLoot(ServerLevel level, BlockPos pos, RandomSource random) {
        int roll = random.nextInt(120);
        if (roll < 30) {
            return new SnifferDig(new ItemStack(Items.TORCHFLOWER_SEEDS), 20 * 20);
        }
        if (roll < 42) {
            return new SnifferDig(new ItemStack(Items.PITCHER_POD), 20 * 20);
        }
        if (roll < 58) {
            return new SnifferDig(new ItemStack(Items.COAL), 8 * 20);
        }
        if (roll < 72) {
            return new SnifferDig(new ItemStack(Items.IRON_NUGGET, 2), 8 * 20);
        }
        if (roll < 80) {
            return new SnifferDig(new ItemStack(Items.GOLD_NUGGET, 2), 15 * 20);
        }
        if (roll < 88) {
            return new SnifferDig(new ItemStack(Items.BONE), 10 * 20);
        }
        if (roll < 100) {
            return new SnifferDig(new ItemStack(Items.BONE_MEAL, 2), 5 * 20);
        }
        if (roll < 110) {
            Item[] sherds = sherdPoolAt(level, pos).sherds;
            return new SnifferDig(new ItemStack(sherds[random.nextInt(sherds.length)]), 15 * 20);
        }
        if (roll < 118) {
            // Graceful degradation: with Immortalers Delight absent the seed
            // resolves to air, so fall back to coal instead of charging the
            // player 25s of duration for nothing.
            ItemStack seeds = rollAncientSeed(random);
            return seeds.isEmpty()
                    ? new SnifferDig(new ItemStack(Items.COAL), 8 * 20)
                    : new SnifferDig(seeds, 25 * 20);
        }
        return new SnifferDig(new ItemStack(Items.SNIFFER_EGG), 60 * 20);
    }

    /** One of the six ancient seeds, or nothing when the mod is absent. */
    private static ItemStack rollAncientSeed(RandomSource random) {
        Item item = BuiltInRegistries.ITEM.get(ANCIENT_SEED_IDS[random.nextInt(ANCIENT_SEED_IDS.length)]);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ==================================================================
    // Drink tooltips: mechanical description and instant labels
    // ==================================================================

    /**
     * Cookery renders each drink's maxim (dark gray, italic) followed by the
     * vanilla-style effect list with durations. Two touches are added here:
     * <ul>
     *   <li>A one-line mechanical description right below the maxim, in plain
     *       gray - clearly a different voice than the italic quote.</li>
     *   <li>The three instant marker effects carry duration 1, and the
     *       vanilla potion tooltip prints no time for those - they get an
     *       "(instant)" label so every effect line notes how long it lasts.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(KaleidoscopeFlora.MOD_ID)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();

        Component description = Component.translatable(
                "tooltip." + KaleidoscopeFlora.MOD_ID + "." + id.getPath() + ".desc")
                .withStyle(ChatFormatting.GRAY);
        // Insert right below the maxim: Cookery ends the quote with a blank
        // line before the effect list, so the first blank line is the anchor.
        int insertAt = 1;
        for (int i = 1; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().isBlank()) {
                insertAt = i;
                break;
            }
        }
        tooltip.add(Math.min(insertAt, tooltip.size()), description);

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getContents() instanceof TranslatableContents contents
                    && INSTANT_EFFECT_KEYS.contains(contents.getKey())) {
                tooltip.set(i, line.copy().append(
                        Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + ".instant")));
            }
        }

        // Rename Cookery's creative-tab attribution for flora drinks: the
        // drinks stay in Cookery's "美食" tab, but the blue hint line should
        // speak for this mod. Cookery (the alphabetically earlier mod id)
        // adds its line before this handler sees it.
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("item_group.kaleidoscope_cookery.cookery_food.name")) {
                tooltip.set(i, Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + ".group")
                        .setStyle(line.getStyle()));
                break;
            }
        }
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    /**
     * Re-adds an effect with the given cost subtracted from its remaining
     * duration (durations are immutable on live instances). The effect
     * simply ends when the cost exceeds what is left.
     */
    private static void shortenEffect(LivingEntity target, Holder<MobEffect> effect, int costTicks) {
        MobEffectInstance instance = target.getEffect(effect);
        if (instance == null) {
            return;
        }
        int remaining = instance.getDuration() - costTicks;
        target.removeEffect(effect);
        if (remaining > 0) {
            target.addEffect(new MobEffectInstance(instance.getEffect(), remaining,
                    instance.getAmplifier(), instance.isAmbient(), instance.isVisible()));
        }
    }

    /** Cookery's warmth effect holder, resolved once (Cookery is a hard dependency). */
    private static boolean hasWarmth(LivingEntity entity) {
        if (warmthEffect == null) {
            warmthEffect = BuiltInRegistries.MOB_EFFECT.getHolder(
                    ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "warmth")).orElse(null);
        }
        return warmthEffect != null && entity.hasEffect(warmthEffect);
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    /**
     * The player's horizontal movement vector (dx, dz) of the last server
     * tick. Player movement is client authoritative, so on the server
     * {@code getDeltaMovement()} and {@code walkDist} are always ~0 for
     * players - effects that need the real step read it from here. One tick
     * of latency is inherent (positions are diffed after the player tick).
     */
    static double[] lastMove(Player player) {
        double[] delta = MOVE_DELTA.get(player.getUUID());
        return delta == null ? new double[]{0.0, 0.0} : delta;
    }

    /** Frees the per-entity cooldown maps when a player leaves. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        KISS_COOLDOWN.remove(uuid);
        ECHO_GRACE_UNTIL.remove(uuid);
        LAST_XZ.remove(uuid);
        MOVE_DELTA.remove(uuid);
        LUCK_START.remove(uuid);
        ModEffects.FlowerPathEffect.clearPlayer(uuid);
        ModEffects.GazeEffect.clearPlayer(uuid);
        ModEffects.PetalWalkEffect.clearPlayer(uuid);
        FloraAdvancements.clearPlayer(uuid);
        clearBrushSession(uuid);
    }
}
