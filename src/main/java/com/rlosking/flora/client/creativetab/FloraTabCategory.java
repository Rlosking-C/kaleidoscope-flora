package com.rlosking.flora.client.creativetab;

import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import com.rlosking.flora.BlossomMooncakes;
import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.FloraAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public enum FloraTabCategory {

    DRINKS("drinks", () -> new ItemStack(TeacupRegistry.getItem(
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "lullaby")))),

    OTHER("other", () -> new ItemStack(BlossomMooncakes.ITEM.get()));

    private final String key;
    private final Supplier<ItemStack> icon;

    FloraTabCategory(String key, Supplier<ItemStack> icon) {
        this.key = key;
        this.icon = icon;
    }

    public ItemStack icon() {
        return icon.get();
    }

    public Component label() {
        return Component.translatable("tooltip.kaleidoscope_flora.tab." + key);
    }

    public boolean accepts(ItemStack stack) {
        if (this == DRINKS) {
            return FloraAdvancements.isFloraDrink(stack);
        }
        return !FloraAdvancements.isFloraDrink(stack);
    }
}
