package com.rlosking.flora.soupbase;

import com.github.ysbbbbbb.kaleidoscopecookery.api.client.render.ISoupBaseRender;
import com.github.ysbbbbbb.kaleidoscopecookery.api.recipe.soupbase.ISoupBase;
import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.client.soupbase.MilkSoupBaseRender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Milk as a stockpot soup base. Cookery ships no milk base (only water, lava
 * and mob buckets), and its {@code FluidSoupBase} cannot be reused because a
 * milk bucket carries {@code Fluids.EMPTY}, which has no still texture. This
 * implementation pours from a milk bucket, hands back the empty bucket, and
 * renders the milk surface from our own animation strip.
 */
public class MilkSoupBase implements ISoupBase {
    /** Milk-white tint used for the bubbling particles above the pot. */
    public static final int MILK_BUBBLE_COLOR = 0xFFF6E8;

    /** Registry id this base is filed under in the {@code SoupBaseManager}. */
    @Override
    public ResourceLocation getName() {
        return KaleidoscopeFlora.MILK_SOUP_BASE;
    }

    @Override
    public int getBubbleColor() {
        return MILK_BUBBLE_COLOR;
    }

    /** The stack shown in JEI and the recipe book to represent this base. */
    @Override
    public ItemStack getDisplayStack() {
        return new ItemStack(Items.MILK_BUCKET);
    }

    /** Only a milk bucket can be poured in as this soup base. */
    @Override
    public boolean isSoupBase(ItemStack stack) {
        return stack.is(Items.MILK_BUCKET);
    }

    /**
     * Called when the player pours the base INTO the pot: plays the
     * bucket-empty sound and hands back the empty bucket.
     */
    @Override
    public ItemStack getReturnContainer(Level level, LivingEntity entity, ItemStack stack) {
        playPourSound(level, entity, SoundEvents.BUCKET_EMPTY);
        return new ItemStack(Items.BUCKET);
    }

    /** A player holding an empty bucket can scoop the milk back out. */
    @Override
    public boolean isContainer(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }

    /**
     * Called when the player scoops the base BACK OUT with a matching
     * container: plays the bucket-fill sound and returns a fresh milk bucket.
     */
    @Override
    public ItemStack getReturnSoupBase(Level level, LivingEntity entity, ItemStack stack) {
        playPourSound(level, entity, SoundEvents.BUCKET_FILL);
        return new ItemStack(Items.MILK_BUCKET);
    }

    /**
     * Our own renderer: milk waves for the ingredient stage, recipe visuals after.
     * Marked client-only like Cookery's own soup bases: RuntimeDistCleaner strips
     * this method on the dedicated server so the render class is never loaded there.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public ISoupBaseRender getRender() {
        return new MilkSoupBaseRender();
    }

    /** Shared pour/scoop sound helper - heard by everyone nearby. */
    private static void playPourSound(Level level, LivingEntity entity, SoundEvent sound) {
        level.playSound(null, entity.blockPosition(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
