package com.rlosking.flora.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.rlosking.flora.block.FlowerCakeBlock;

public abstract class PlaceableCakeItem extends BlockItem {

    protected PlaceableCakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).is(getBlock())) {
            return super.useOn(context);
        }
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(getBlock())) {
            int count = state.getValue(FlowerCakeBlock.STACK_COUNT);
            if (count >= 4) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(FlowerCakeBlock.STACK_COUNT, count + 1),
                        Block.UPDATE_ALL);
                Player player = context.getPlayer();
                if (player == null || !player.isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.place(context);
    }
}
