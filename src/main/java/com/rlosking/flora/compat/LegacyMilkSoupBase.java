package com.rlosking.flora.compat;

import com.github.ysbbbbbb.kaleidoscopecookery.api.client.render.ISoupBaseRender;
import com.github.ysbbbbbb.kaleidoscopecookery.api.recipe.soupbase.ISoupBase;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class LegacyMilkSoupBase implements ISoupBase {
    private static final ResourceLocation NATIVE_MILK = ResourceLocation.withDefaultNamespace("milk");

    private static ISoupBase milk() {
        return SoupBaseManager.getSoupBase(NATIVE_MILK);
    }

    @Override
    public ResourceLocation getName() {
        return KaleidoscopeFlora.MILK_SOUP_BASE;
    }

    @Override
    public int getBubbleColor() {
        return milk().getBubbleColor();
    }

    @Override
    public ItemStack getDisplayStack() {
        return milk().getDisplayStack();
    }

    @Override
    public boolean isSoupBase(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack getReturnContainer(Level level, LivingEntity entity, ItemStack stack) {
        return milk().getReturnContainer(level, entity, stack);
    }

    @Override
    public boolean isContainer(ItemStack stack) {
        return milk().isContainer(stack);
    }

    @Override
    public ItemStack getReturnSoupBase(Level level, LivingEntity entity, ItemStack stack) {
        return milk().getReturnSoupBase(level, entity, stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISoupBaseRender getRender() {
        return milk().getRender();
    }
}
