package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mooncake item: edible, and placeable onto a tray where it stacks with any
 * mooncake already there.
 *
 * <p>Eating one turns the air into water for a while - the eater swims
 * through it, rises by looking up, and takes no fall damage aloft (see
 * {@link ModEffects#FLOATING}). Feeding it is worth a bread: 5 hunger, 6
 * saturation.</p>
 *
 * <p>The moon is generous about the duration: five minutes of drifting by
 * day, ten by night. The night runs on the level's day time, so a mooncake
 * eaten just before dawn keeps its ten minutes.</p>
 */
public class BlossomMooncakeItem extends BlockItem {

    private static final long NIGHT_START = 12000L;
    private static final long DAY_LENGTH = 24000L;

    /** Air-swim time: 5 minutes by day, 10 by night. */
    private static final int DAY_FLOATING_TICKS = 5 * 60 * 20;
    private static final int NIGHT_FLOATING_TICKS = 10 * 60 * 20;

    public BlossomMooncakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Adds to an existing tray stack instead of placing a second block. */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(getBlock())) {
            int count = state.getValue(BlossomMooncakeBlock.STACK_COUNT);
            if (count >= 4) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(BlossomMooncakeBlock.STACK_COUNT, count + 1), Block.UPDATE_ALL);
                Player player = context.getPlayer();
                if (player == null || !player.isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.place(context);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            boolean night = serverLevel.getDayTime() % DAY_LENGTH >= NIGHT_START;
            player.addEffect(new MobEffectInstance(ModEffects.FLOATING,
                    night ? NIGHT_FLOATING_TICKS : DAY_FLOATING_TICKS, 0));
        }
        return result;
    }
}
