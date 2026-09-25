package com.rlosking.flora.soupbase;

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

/**
 * Compatibility alias for worlds saved by Flora 0.3.1 and earlier: a stockpot
 * whose NBT still holds {@code kaleidoscope_flora:milk} resolves through this
 * class to Cookery 1.5.0's native milk base, so those pots keep rendering
 * instead of crashing the client renderer.
 *
 * <p><b>Why this is not part of the stockpot route that was deleted on
 * 2026-09-22.</b> The 26 stockpot recipes went, but this alias is not a recipe
 * and not an ingredient - it is a lookup target for save data that already
 * exists on disk. Cookery's stockpot renderer calls
 * {@code SoupBaseManager.getSoupBase(pot.soupBaseId)} and dereferences the
 * result without a null check, so an id that no longer resolves to anything
 * throws {@code NullPointerException: ... because "soupBase" is null} the
 * moment such a pot is on screen. Removing this class brings that crash back,
 * which is exactly what happened. Do not delete it again.</p>
 *
 * <p>The delegate is resolved lazily per call because Cookery 1.5.0 registers
 * its soup bases during FMLCommonSetupEvent, after all mod constructors - an
 * eager lookup at construction time would see null. Every actual call
 * (render, lookup) happens during gameplay, long after setup.</p>
 */
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

    /**
     * Never claims fresh pours: Cookery's own minecraft:milk entry claims the
     * bucket, and a legacy id would no longer match the Hanami Tale recipe,
     * which now requires minecraft:milk.
     */
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

    /**
     * Delegates to Cookery's milk renderer. Must stay @OnlyIn(CLIENT):
     * RuntimeDistCleaner strips it on the dedicated server, so the stripped
     * delegate method is never invoked there.
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public ISoupBaseRender getRender() {
        return milk().getRender();
    }
}
