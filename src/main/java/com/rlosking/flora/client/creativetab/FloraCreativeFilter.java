package com.rlosking.flora.client.creativetab;

import com.rlosking.flora.init.FloraCreativeTab;
import com.rlosking.flora.mixin.client.CreativeModeInventoryScreenAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class FloraCreativeFilter {

    public static final int COLUMN_X = -28;

    public static final int COLUMN_Y = 17;

    public static final int BUTTON_WIDTH = 32;

    public static final int BUTTON_HEIGHT = 26;

    public static final int BUTTON_PITCH = 27;

    private static FloraTabCategory current = FloraTabCategory.DRINKS;

    public static FloraTabCategory current() {
        return current;
    }

    public static void setCurrent(FloraTabCategory category) {
        current = category;
    }

    public static void apply(CreativeModeInventoryScreen screen) {
        if (!isFloraTabSelected(screen)) {
            return;
        }
        List<ItemStack> catalogue = new ArrayList<>(FloraCreativeTab.FLORA_TAB.get().getDisplayItems());
        NonNullList<ItemStack> shown = screen.getMenu().items;
        shown.clear();
        for (ItemStack stack : catalogue) {
            if (current.accepts(stack)) {
                shown.add(stack);
            }
        }
        screen.getMenu().scrollTo(0.0F);
    }

    public static boolean isFloraTabSelected(CreativeModeInventoryScreen screen) {
        CreativeModeTab selected = CreativeModeInventoryScreenAccessor.flora$selectedTab();
        return selected != null && selected == FloraCreativeTab.FLORA_TAB.get();
    }

    private FloraCreativeFilter() {
    }
}
