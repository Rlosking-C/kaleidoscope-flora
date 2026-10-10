package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/**
 * Base of every Flower Perch block: one registered block per plantable
 * flower (the vanilla potted-flower pattern), each carrying a 0..4 growth
 * stage: 0 planted, 1 sprouting, 2 growing, 3 budding, 4 full bloom.
 *
 * <p>Shears can trim two levels: a full bloom drops 4-5 flowers and reverts
 * to budding (3); a bud drops 1-2 flowers and reverts to growing (2). The
 * flower always grows back, so a perch is a garden, not a click-farm (the
 * Farmer's Delight colony paradigm translated to flowers).</p>
 */
public abstract class FlowerPerchBlock extends Block implements BonemealableBlock {

    /** Growth stages 0..4 (4 = full bloom, shearable). */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);

    /**
     * The wood the perch was built from. Carried through planting, trimming
     * and breaking, so a cherry perch stays a cherry perch - the frame is
     * drawn from vanilla textures chosen by this property (see
     * {@link PerchWood}), and the blockstate composes frame and plant
     * through multipart, which is why one block covers all nine species.
     */
    public static final EnumProperty<PerchWood> WOOD = EnumProperty.create("wood", PerchWood.class);

    /**
     * Whether another perch sits directly on top of this one. The four corner
     * posts are a SEPARATE model that is only applied while this is true - a
     * lone perch is a low tray, two stacked perches become a rack. Vanilla
     * fences work the same way: the blockstate is derived from the neighbours
     * rather than stored permanently (see getStateForPlacement / updateShape).
     */
    public static final BooleanProperty ABOVE = BooleanProperty.create("above");

    /** True while a perch of any kind occupies the block above. */
    public static boolean hasPerchAbove(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.above()).getBlock() instanceof FlowerPerchBlock;
    }

    /**
     * Shared footprint: the whole planter. Collision AND the empty-perch
     * outline. The model is no longer flower-pot sized - the walls run the
     * full 0..16 with corner posts to y=8 and the soil top at y=5 - so the
     * box follows the model at 16x16, 6 tall. Height 6 (0.375) sits under the
     * auto step-up threshold, so walking into a perch steps onto it instead
     * of being blocked, the way a slab behaves.
     */
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    /** Planted outlines: frame + plant volume stacked above y=6. */
    private static final VoxelShape SHAPE_EARLY = Shapes.or(SHAPE, Block.box(5.0, 6.0, 5.0, 11.0, 11.0, 11.0));
    private static final VoxelShape SHAPE_SMALL = Shapes.or(SHAPE, Block.box(5.0, 6.0, 5.0, 11.0, 12.0, 11.0));
    private static final VoxelShape SHAPE_FULL = Shapes.or(SHAPE, Block.box(3.0, 6.0, 3.0, 13.0, 16.0, 13.0));

    /**
     * The four corner posts, y6 -> 16, matching the perch_posts_* models exactly.
     * Only present while ABOVE is true; the base slab already covers y0-6, so
     * this adds only the part above it (the two volumes just touch).
     */
    private static final VoxelShape POSTS = Shapes.or(
            Block.box(0.0, 6.0, 0.0, 2.0, 16.0, 2.0),
            Block.box(14.0, 6.0, 0.0, 16.0, 16.0, 2.0),
            Block.box(0.0, 6.0, 14.0, 2.0, 16.0, 14.0),
            Block.box(14.0, 6.0, 14.0, 16.0, 16.0, 16.0));

    /** Aim points at/above this height (6/16) hit the plant, below it the frame. */
    private static final double PLANT_MIN_Y = 6.0 / 16.0;

    protected FlowerPerchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(STAGE, 0)
                .setValue(WOOD, PerchWood.DEFAULT)
                .setValue(ABOVE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(STAGE, 0)
                .setValue(WOOD, PerchWood.DEFAULT)
                .setValue(ABOVE, hasPerchAbove(context.getLevel(), context.getClickedPos()));
    }

    /** Fence-style: grow/shed the posts when the block above appears or goes. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.UP) {
            return state.setValue(ABOVE, hasPerchAbove(level, pos));
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, WOOD, ABOVE);
    }

    /**
     * Selection outline = frame + plant volume, so the crosshair can target
     * the plant on its own. Collision stays frame-only (see below): the
     * plant never blocks movement, exactly like a vanilla flower.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape frame;
        if (!isPlanted(state)) {
            frame = SHAPE;
        } else {
            frame = switch (state.getValue(STAGE)) {
                case 4 -> isTallFlower() ? SHAPE_SMALL : SHAPE_FULL;
                case 3 -> SHAPE_SMALL;
                default -> SHAPE_EARLY;
            };
        }
        // The posts are part of the block's silhouette while ABOVE is true.
        return state.getValue(ABOVE) ? Shapes.or(frame, POSTS) : frame;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Base slab always; the four posts join it while a perch sits on top, so
        // you can stand on the base and between the posts, and the posts block
        // movement exactly where they are drawn.
        return state.getValue(ABOVE) ? Shapes.or(SHAPE, POSTS) : SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.DOWN);
    }

    // ------------------------------------------------------------------
    // Growth: one random tick may advance the stage. Per-stage time is a
    // random draw between the configured min and max minutes, so no two
    // perches bloom in lockstep.
    // ------------------------------------------------------------------

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isPlanted(state)) {
            return;
        }
        int stage = state.getValue(STAGE);
        if (stage >= 4) {
            return;
        }
        double chance = growthChancePerRandomTick(random);
        if (random.nextDouble() < chance) {
            advanceStage(level, pos, state, stage);
        }
    }

    /**
     * Probability that one random tick advances one stage. Random ticks
     * arrive every 4096 ticks on average, so p = 4096 / (minutes * 60 * 20)
     * with minutes drawn uniformly from the configured [min, max] range.
     */
    private static double growthChancePerRandomTick(RandomSource random) {
        double min = FloraConfig.SPEC.isLoaded() ? FloraConfig.perchGrowthMinutesMin() : 3.0;
        double max = FloraConfig.SPEC.isLoaded() ? FloraConfig.perchGrowthMinutesMax() : 5.0;
        double minutes = min + random.nextDouble() * Math.max(0.0, max - min);
        double expectedTicks = Math.max(1.0, minutes * 60.0 * 20.0);
        return Math.min(1.0, 4096.0 / expectedTicks);
    }

    /** Sets the next stage and handles everything that happens on reaching full bloom. */
    private void advanceStage(Level level, BlockPos pos, BlockState state, int stage) {
        level.setBlock(pos, state.setValue(STAGE, stage + 1), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.4F, 0.9F);
        if (stage + 1 == 4) {
            onFullBloom(level, pos);
        }
    }

    /**
     * A perch reached full bloom: award bloom progress to every player
     * standing nearby (random ticks carry no player context, so nearby is
     * the only fair attribution for both growth and bone meal paths).
     */
    protected void onFullBloom(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || plantedFlowerId() == null) {
            return;
        }
        for (Player player : server.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) {
                FloraAdvancements.perchBloomed((net.minecraft.server.level.ServerPlayer) player, plantedFlowerId());
            }
        }
    }

    // ------------------------------------------------------------------
    // Interaction: shears (two trim tiers). Bone meal deliberately is NOT
    // handled here - vanilla routes it through BonemealableBlock below,
    // which consumes the item and plays the standard green particles.
    // ------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.SHEARS) && isPlanted(state)) {
            int stage = state.getValue(STAGE);
            if (stage == 4) {
                if (!level.isClientSide) {
                    popFlower(level, pos, state, 4 + level.random.nextInt(2));
                    level.setBlock(pos, state.setValue(STAGE, 3), Block.UPDATE_CLIENTS);
                    damageShears(stack, player, hand);
                    level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.7F, 1.2F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (stage == 3) {
                if (!level.isClientSide) {
                    popFlower(level, pos, state, 1 + level.random.nextInt(2));
                    level.setBlock(pos, state.setValue(STAGE, 2), Block.UPDATE_CLIENTS);
                    damageShears(stack, player, hand);
                    level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.6F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    private static void damageShears(ItemStack stack, Player player, InteractionHand hand) {
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
    }

    /** Spawns the planted flower's item plus a puff of petals in its colour. */
    protected void popFlower(Level level, BlockPos pos, BlockState state, int count) {
        Item flower = plantedFlowerItem(state);
        if (flower != null) {
            popResource(level, pos, new ItemStack(flower, count));
            if (level instanceof ServerLevel server) {
                int c = petalColor();
                DustParticleOptions options = new DustParticleOptions(toVector(c), 0.7F);
                server.sendParticles(options, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                        6, 0.2, 0.15, 0.2, 0.02);
            }
        }
    }

    // ------------------------------------------------------------------
    // Left-click: the plant and the frame are two selectable sub-volumes
    // (outline covers both, collision only the frame). Aiming at the plant
    // breaks ONLY the plant - flowers drop by stage, an empty perch stays.
    // Aiming at the frame falls through to the vanilla break.
    //
    // Two hardening details, both learned from NeoForge's actual patches:
    // - The aim is re-derived by clipping the block's OWN outline shape, not
    //   the world ray (other blocks in the ray - e.g. a glass test box -
    //   would skew a world clip into "not this block" and unleash the
    //   vanilla whole-block break).
    // - While the attack button is held, the client re-fires START for every
    //   NEW block under the crosshair; the freshly emptied perch is exactly
    //   such a new (instabreak) block and would be chain-broken before the
    //   player releases. A short guard window swallows that follow-up.
    // ------------------------------------------------------------------

    private record GuardKey(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** Plant-break timestamps (per side) feeding the held-button guard. */
    private static final java.util.Map<GuardKey, Long> PLANT_BREAKS = new java.util.HashMap<>();
    private static final int PLANT_BREAK_GUARD_TICKS = 10;

    /** Subscribes the plant-break handler on the game event bus. */
    public static void registerGameEvents() {
        NeoForge.EVENT_BUS.addListener(FlowerPerchBlock::onLeftClickBlock);
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FlowerPerchBlock perch)) {
            return;
        }
        long now = level.getGameTime();
        if (perch.isPlanted(state)) {
            Player player = event.getEntity();
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
            BlockHitResult hit = state.getShape(level, pos).clip(eye, end, pos);
            if (hit.getType() == HitResult.Type.BLOCK
                    && hit.getLocation().y - pos.getY() >= PLANT_MIN_Y) {
                event.setCanceled(true);
                PLANT_BREAKS.put(new GuardKey(level.dimension(), pos), now);
                if (!level.isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        perch.popFlower(level, pos, state, flowerDropCount(state.getValue(STAGE), level.random));
                    }
                    level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
                    level.setBlock(pos, FlowerPerches.EMPTY_PERCH.get().defaultBlockState()
                            .setValue(WOOD, state.getValue(WOOD)), Block.UPDATE_ALL);
                }
            }
        } else if (isFreshlyEmptiedPerch(level, pos, now)) {
            event.setCanceled(true);
            if (!level.isClientSide && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(pos, state));
            }
        }
    }

    /** True within the guard window after this pos was emptied by a plant break. */
    private static boolean isFreshlyEmptiedPerch(Level level, BlockPos pos, long now) {
        GuardKey key = new GuardKey(level.dimension(), pos);
        Long at = PLANT_BREAKS.get(key);
        if (at == null) {
            return false;
        }
        if (now - at > PLANT_BREAK_GUARD_TICKS) {
            PLANT_BREAKS.remove(key);
            return false;
        }
        return true;
    }

    /** Breaking the plant yields the same flowers shears would at that stage. */
    protected static int flowerDropCount(int stage, RandomSource random) {
        return switch (stage) {
            case 4 -> 4 + random.nextInt(2);
            case 3 -> 1 + random.nextInt(2);
            default -> 1;
        };
    }

    // ------------------------------------------------------------------
    // Breaking: no loot tables - both the perch and the flower pop as items
    // from code (the vanilla flower-pot pattern), so VB flowers absent from
    // the install can never crash a loot context.
    // ------------------------------------------------------------------

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!player.getAbilities().instabuild) {
            dropPerchContents(level, pos, state);
        }
    }

    /** Pops the empty perch (same wood it was built from) plus the stage-matched flower yield, no loot table. */
    protected void dropPerchContents(Level level, BlockPos pos, BlockState state) {
        popResource(level, pos, new ItemStack(FlowerPerches.emptyPerchItem(state.getValue(WOOD)).get()));
        Item flower = plantedFlowerItem(state);
        if (flower != null) {
            popResource(level, pos, new ItemStack(flower, flowerDropCount(state.getValue(STAGE), level.random)));
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!state.canSurvive(level, pos)) {
            dropPerchContents(level, pos, state);
            level.removeBlock(pos, false);
        }
    }

    // ------------------------------------------------------------------
    // BonemealableBlock: advances one stage until the bloom.
    // ------------------------------------------------------------------

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return isPlanted(state) && state.getValue(STAGE) < 4;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int stage = state.getValue(STAGE);
        advanceStage(level, pos, state, stage);
    }

    // ------------------------------------------------------------------
    // Falling petal shower on bloomed perches (client). Custom petal
    // particles (16x16 silhouettes) tinted with the flower's colour: each
    // petal falls, sways and tumbles, then disappears the moment it lands.
    // v0.4.0: quadSize 0.05, lifetime 300 ticks, and an 8-per-tick budget
    // shared by every perch nearby - see the budget fields below.
    // ------------------------------------------------------------------

    /** Client-side petal budget, shared by every perch in the level.
     *  animateTick already only runs for blocks near the player (the distance
     *  gate is free); this caps the AGGREGATE, so ten perches side by side
     *  cannot stack ten showers. The client is single-threaded, so no sync is
     *  needed. No debt is carried across ticks - a burst must never build up. */
    private static long aromaBudgetTick = Long.MIN_VALUE;
    private static int aromaBudgetUsed = 0;
    private static final int AROMA_BUDGET_PER_TICK = 8;

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) != 4) {
            return;
        }
        long now = level.getGameTime();
        if (now != aromaBudgetTick) {
            aromaBudgetTick = now;
            aromaBudgetUsed = 0;
        }
        int budget = AROMA_BUDGET_PER_TICK - aromaBudgetUsed;
        if (budget <= 0) {
            return;
        }
        // Sparse and elegant: ~1.5 petals per tick drift down from just
        // above the bloom. Fractional densities round stochastically so
        // low values keep feeling alive instead of pulsing in bursts.
        double density = FloraConfig.SPEC.isLoaded() ? FloraConfig.perchAromaDensity() : 1.5;
        int petals = (int) Math.floor(density)
                + (random.nextDouble() < (density - Math.floor(density)) ? 1 : 0);
        if (petals <= 0) {
            return;
        }
        petals = Math.min(petals, budget);
        aromaBudgetUsed += petals;
        double range = FloraConfig.SPEC.isLoaded() ? FloraConfig.perchAromaRange() : 16.0;
        for (int i = 0; i < petals; i++) {
            level.addParticle(FloraParticleTypes.aroma(petalColor(), 1.0F),
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * range * 2.0,
                    pos.getY() + 2.0 + random.nextDouble() * 6.0,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * range * 2.0,
                    (random.nextDouble() - 0.5) * 0.02,  // faint shared breeze
                    -0.02,
                    (random.nextDouble() - 0.5) * 0.02);
        }
    }

    private static Vector3f toVector(int rgb) {
        return new Vector3f(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F);
    }

    /** True when this blockstate belongs to a planted (non-empty) perch. */
    protected abstract boolean isPlanted(BlockState state);

    /** The flower item this perch carries, or null when empty/unavailable. */
    @Nullable
    protected abstract Item plantedFlowerItem(BlockState state);

    /** Registry id of the planted flower item, or null (advancement bookkeeping). */
    @Nullable
    protected abstract net.minecraft.resources.ResourceLocation plantedFlowerId();

    /** Main colour of the planted flower, used for petal particles. */
    protected abstract int petalColor();

    /** Tall flowers keep the small (0.6) silhouette at full bloom. */
    protected boolean isTallFlower() {
        return false;
    }

    /** The wood this perch was built from (see {@link PerchWood}). */
    public PerchWood woodOf(BlockState state) {
        return state.getValue(WOOD);
    }

    /**
     * What creative middle-click (pick block) hands back.
     *
     * <p>NeoForge routes pick-block through {@code IBlockStateExtension#
     * getCloneItemStack(HitResult, LevelReader, BlockPos, Player)}, whose
     * default is this three-argument method, and {@code Block}'s own default
     * is {@code new ItemStack(this)} - i.e. {@code Item.byBlock(this)}. All
     * nine empty-perch items are BlockItems of the SAME block (so that one
     * block covers every species), which leaves {@code Item.BY_BLOCK} with a
     * single entry, written by whichever item registered last: pale oak. The
     * result was that picking an oak perch produced a pale-oak perch.</p>
     *
     * <p>So the species has to come from the blockstate, exactly like
     * {@code FlowerPotBlock} returns its potted plant instead of the pot.
     * An empty perch yields the item of its own wood; a planted one yields
     * the flower it carries (vanilla parity), falling back to the frame item
     * if that flower is a VanillaBackport one and VB is absent - never air.</p>
     *
     * <p>Deliberately the three-argument overload, not NeoForge's five-argument
     * one: the five-argument default delegates here, and vanilla's own
     * {@code FlowerPotBlock} overrides this one, so every call path is covered.
     * The method carries Forge's {@code @Deprecated} hint aimed at callers, who
     * may want the hit target and player - an overrider has no more sensitive
     * overload to use instead.</p>
     */
    @Override
    @SuppressWarnings("deprecation")
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        Item flower = isPlanted(state) ? plantedFlowerItem(state) : null;
        return flower != null
                ? new ItemStack(flower)
                : new ItemStack(FlowerPerches.emptyPerchItem(state.getValue(WOOD)).get());
    }
}
