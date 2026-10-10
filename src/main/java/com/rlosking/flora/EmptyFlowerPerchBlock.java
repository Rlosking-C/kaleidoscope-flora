package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The empty perch: crafting output, stage never moves, accepts a plantable
 * flower. Planting is a block swap into the matching
 * {@link PlantedFlowerPerchBlock} - the exact mechanism vanilla uses for
 * "flower pot becomes potted flower", so no dynamic properties exist.
 */
public class EmptyFlowerPerchBlock extends FlowerPerchBlock {

    public EmptyFlowerPerchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        Block perch = FlowerPerches.perchFor(stack);
        if (perch != null) {
            if (!level.isClientSide) {
                level.setBlock(pos, perch.defaultBlockState()
                        .setValue(FlowerPerchBlock.WOOD, state.getValue(FlowerPerchBlock.WOOD))
                        .setValue(FlowerPerchBlock.ABOVE,
                                FlowerPerchBlock.hasPerchAbove(level, pos)), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.7F, 0.8F);
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    FloraAdvancements.perchPlanted(serverPlayer);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected boolean isPlanted(BlockState state) {
        return false;
    }

    @Override
    @Nullable
    protected Item plantedFlowerItem(BlockState state) {
        return null;
    }

    @Override
    @Nullable
    protected ResourceLocation plantedFlowerId() {
        return null;
    }

    @Override
    protected int petalColor() {
        return 0xFFFFFF;
    }
}
