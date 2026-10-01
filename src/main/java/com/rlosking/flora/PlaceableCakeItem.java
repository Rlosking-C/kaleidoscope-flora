package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared placement behaviour for the three flower cakes: <b>adding to a cake
 * already on the ground is a plain right-click, and setting the first one down
 * on an ordinary block is a sneak + right-click.</b>
 *
 * <p><b>Two different gestures, on purpose.</b> Adding to a stack that is
 * already there is the mooncake's behaviour word for word - walk up to a placed
 * cake and right-click, no modifier - because that is a continuation of
 * something the player already started, and asking for a sneak there is
 * friction with no purpose. Starting a new pile is different: a flower cake's
 * ordinary right-click is "eat it" (or, for the raw one, "throw it"), so the
 * first placement is the modifier action.</p>
 *
 * <p><b>How "ordinary right-click still eats" is preserved.</b> When the player
 * is not sneaking <em>and not aiming at a cake</em>, this returns
 * {@link InteractionResult#PASS}, and the client falls back to using the item
 * normally - the same mechanism that lets a vanilla player eat bread while
 * looking straight at a block, or throw a snowball at a wall. Nothing about
 * eating or throwing is reimplemented here.</p>
 *
 * <p><b>Why aiming at a cake wins over eating.</b> Right-clicking a placed cake
 * while holding one therefore stacks rather than eats. That is the mooncake's
 * trade-off exactly, and it is what was asked for; eating is still one step away
 * by aiming anywhere else.</p>
 *
 * <p><b>The stacking half is the mooncake's, generalised.</b>
 * {@link BlossomMooncakeItem#place} adds to the stack already on the block and
 * otherwise defers to {@code BlockItem}; this does the same, with the block
 * taken from {@link BlockItem#getBlock()} so one class serves all three
 * cakes.</p>
 */
public abstract class PlaceableCakeItem extends BlockItem {

    protected PlaceableCakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * Aiming at this cake's own block hands straight to the normal placement
     * path (which is the stacking branch in {@link #place}). Anything else needs
     * a sneak, and without one this passes so the item's own {@code use} - eat,
     * or throw - still runs.
     */
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

    /** Adds to an existing stack instead of placing a second block. */
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
