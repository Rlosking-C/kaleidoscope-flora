package com.rlosking.flora.block;

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
import com.rlosking.flora.init.BlossomMooncakes;

public class BlossomMooncakeBlock extends Block {

    public static final IntegerProperty STACK_COUNT = IntegerProperty.create("stack_count", 0, 4);

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0);

    public BlossomMooncakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STACK_COUNT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STACK_COUNT);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().getItem() == BlossomMooncakes.ITEM.get()
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
            ItemStack taken = new ItemStack(BlossomMooncakes.ITEM.get());
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos,
                                ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        popResource(level, pos, new ItemStack(BlossomMooncakes.ITEM.get(), state.getValue(STACK_COUNT) + 1));
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
