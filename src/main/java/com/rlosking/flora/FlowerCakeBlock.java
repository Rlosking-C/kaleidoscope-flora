package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A flower cake set down in the world, where up to five stack on one block.
 *
 * <p>Directly modelled on {@link BlossomMooncakeBlock}: same {@code stack_count}
 * property 0-4 (one less than the number of cakes, so state 0 is a single cake),
 * same take-one-back with an empty hand, same "breaking returns everything"
 * drop. The user asked for exactly that ("the placement code should reuse the
 * mooncake"), and the mooncake's version is already in the field.</p>
 *
 * <p><b>One class for all three cakes.</b> The mooncake could hard-code its item
 * because there was only one; here there are three, so the "is the right item in
 * hand" test goes through {@link #asItem()} - the block's own item form -
 * instead of naming one. That also avoids a circular reference: the item is
 * built from the block, so the block cannot name the item.</p>
 *
 * <p><b>Placing needs a sneak, unlike the mooncake.</b> That gate lives in
 * {@link PlaceableCakeItem}, not here: a flower cake's ordinary right-click is
 * "eat it" (or, for the raw one, "throw it"), so placing has to be the modifier
 * action rather than the default one.</p>
 */
public class FlowerCakeBlock extends Block {

    /** Cakes on the block, minus one: 0 = one cake, 4 = five. */
    public static final IntegerProperty STACK_COUNT = IntegerProperty.create("stack_count", 0, 4);

    /**
     * Footprint of the plate, matching {@code BlossomMooncakeBlock}'s tray: the
     * cake models sit inside 14x14 at x/z 1 and no state is taller than four
     * pixels.
     */
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0);

    public FlowerCakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STACK_COUNT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STACK_COUNT);
    }

    /** Only this block's own cake adds to it, and only while the stack has room. */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().getItem() == this.asItem()
                && state.getValue(STACK_COUNT) < 4;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.setValue(STACK_COUNT, Math.min(existing.getValue(STACK_COUNT) + 1, 4));
        }
        return super.getStateForPlacement(context);
    }

    /** An empty hand takes one cake back off the block; the last one removes it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int count = state.getValue(STACK_COUNT);
            if (count > 0) {
                level.setBlock(pos, state.setValue(STACK_COUNT, count - 1), Block.UPDATE_ALL);
            } else {
                level.removeBlock(pos, false);
            }
            ItemStack taken = new ItemStack(asItem());
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Breaking the block returns every cake still on it. */
    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos,
                                ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        popResource(level, pos, new ItemStack(asItem(), state.getValue(STACK_COUNT) + 1));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
