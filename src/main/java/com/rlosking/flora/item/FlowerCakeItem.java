package com.rlosking.flora.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.rlosking.flora.effect.ModEffects;
import com.rlosking.flora.init.FlowerCakes;
import com.rlosking.flora.FloraConfig;

public class FlowerCakeItem extends PlaceableCakeItem {

    private static final int TOASTED_TICKS = 40 * 20;

    private static final int DEW_TICKS = 60 * 20;

    public FlowerCakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean toasted = stack.is(FlowerCakes.TOASTED.get());
        boolean dew = stack.is(FlowerCakes.DEW.get());
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!(level instanceof ServerLevel) || !(entity instanceof Player eater)) {
            return result;
        }
        if (toasted) {
            eatToasted(eater);
        } else if (dew) {
            eatDew(eater);
        }
        return result;
    }

    private void eatToasted(Player eater) {
        eater.addEffect(new MobEffectInstance(ModEffects.TOASTED_CAKE, TOASTED_TICKS, 0, false, false));
        eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, TOASTED_TICKS, 0));
        eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, TOASTED_TICKS, 0));
        eater.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, TOASTED_TICKS, 0));
    }

    private void eatDew(Player eater) {
        int extraJumps = Math.max(0, FloraConfig.doubleJumpCount());
        eater.addEffect(new MobEffectInstance(ModEffects.DEW_CAKE, DEW_TICKS, Math.max(0, extraJumps - 1)));
    }
}
