package com.rlosking.flora.client.creativetab;

import com.rlosking.flora.FloraCreativeTab;
import com.rlosking.flora.mixin.client.CreativeModeInventoryScreenAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Which section of the flora creative tab is being shown, and the one operation
 * that applies it.
 *
 * <p><b>Where the item list actually lives.</b> The creative screen keeps the
 * displayed items in {@code CreativeModeInventoryScreen.ItemPickerMenu#items} -
 * a public, mutable {@code NonNullList}. Vanilla itself rewrites it in place:
 * that is how the search box filters. So filtering by section is the same
 * operation vanilla already performs, pointed at a different predicate, and it
 * needs no new screen or menu of our own.</p>
 *
 * <p><b>Filtering from the full list, never from the filtered one.</b>
 * {@link #apply} always rebuilds from {@code CreativeModeTab#getDisplayItems()},
 * which is the untouched catalogue. Filtering the current contents instead would
 * make the result depend on the order the buttons were pressed in (pick "drinks",
 * then "other", and you would get nothing).</p>
 *
 * <p><b>No padding to a full grid.</b> A short list is left short, exactly as
 * vanilla's search leaves it - the menu copes, and padding with empty stacks
 * would fill the scrollable area with blanks.</p>
 */
public final class FloraCreativeFilter {

    /**
     * Placement of the side column, copied from the reference mod's column.
     *
     * <p>The author asked for that column reproduced as closely as possible, so
     * these are its numbers rather than invented ones: the column starts 28
     * pixels left of the panel, the first button 17 pixels below the panel's top,
     * and the pitch is 27 (a 26-pixel button plus a 1-pixel gap). Because a
     * button is 32 wide and starts at -28, its right edge reaches 4 pixels
     * <em>into</em> the panel - that overlap is what makes the column read as
     * tabs attached to the panel rather than as boxes floating beside it.</p>
     *
     * <p>Earlier values here were 22/18 with a 20-pixel square button, guessed
     * before the reference's source was available; comparing the two side by side
     * is what settled them.</p>
     */
    public static final int COLUMN_X = -28;

    /** Top of the first button, relative to the panel's top edge. */
    public static final int COLUMN_Y = 17;

    /** Button size. Wider than tall, like the reference's. */
    public static final int BUTTON_WIDTH = 32;

    public static final int BUTTON_HEIGHT = 26;

    /** Distance between the tops of consecutive buttons. */
    public static final int BUTTON_PITCH = 27;

    private static FloraTabCategory current = FloraTabCategory.DRINKS;

    public static FloraTabCategory current() {
        return current;
    }

    public static void setCurrent(FloraTabCategory category) {
        current = category;
    }

    /**
     * Rewrites the screen's displayed items to match the current section.
     *
     * <p>Does nothing unless the flora tab is the one on screen: every other tab
     * has its own catalogue and must be left exactly as vanilla built it.</p>
     */
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


    /** True when this mod's tab is the creative screen's current tab. */
    public static boolean isFloraTabSelected(CreativeModeInventoryScreen screen) {
        CreativeModeTab selected = CreativeModeInventoryScreenAccessor.flora$selectedTab();
        return selected != null && selected == FloraCreativeTab.FLORA_TAB.get();
    }

    private FloraCreativeFilter() {
    }
}
