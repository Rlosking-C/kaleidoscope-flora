package com.rlosking.flora.effect;

import com.rlosking.flora.init.FloraAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class SproutEffect extends MobEffect {
    public SproutEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7BBF4A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        ServerLevel server = (ServerLevel) entity.level();
        BlockPos center = entity.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
            if (entity.getRandom().nextFloat() >= 0.25f) {
                continue;
            }
            if (!ripenOne(server, pos, entity)) {
                continue;
            }
            if (entity instanceof ServerPlayer player) {
                FloraAdvancements.sproutCropRipened(player);
            }
        }
        return true;
    }

    private static boolean ripenOne(ServerLevel level, BlockPos pos, LivingEntity entity) {
        BlockState state = level.getBlockState(pos);
        boolean cropLike = state.getBlock() instanceof CropBlock || ageProperty(state) != null;
        if (!cropLike) {
            return false;
        }
        if (state.getBlock() instanceof CropBlock && state.hasProperty(CropBlock.AGE)) {
            int age = state.getValue(CropBlock.AGE);
            if (age >= MAX_CROP_AGE) {
                return false;
            }
            level.setBlock(pos, state.setValue(CropBlock.AGE, age + 1), 2);
            return true;
        }
        if (!(state.getBlock() instanceof BonemealableBlock growable)) {
            return false;
        }
        if (!growable.isValidBonemealTarget(level, pos, state)
                || !growable.isBonemealSuccess(level, entity.getRandom(), pos, state)) {
            return false;
        }
        growable.performBonemeal(level, entity.getRandom(), pos, state);
        return true;
    }

    private static final int MAX_CROP_AGE = CropBlock.AGE.getPossibleValues().stream()
            .mapToInt(Integer::intValue).max().orElse(7);

    private static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && "age".equals(property.getName())) {
                return age;
            }
        }
        return null;
    }
}
