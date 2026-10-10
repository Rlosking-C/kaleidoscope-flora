package com.rlosking.flora.client.creativetab;

import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import com.rlosking.flora.BlossomMooncakes;
import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.FloraAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The sections of this mod's creative tab, as shown in its side column.
 *
 * <p><b>Two sections, matching the reference mod's column.</b> The design asked
 * for "flower drinks and the rest, since there is not enough content yet", and
 * the reference tab this column is modelled on carries exactly two buttons. A
 * third "all" entry was tried and removed: with the reference's layout the first
 * section is the one shown when the tab opens, so the column is a strict
 * either/or, and an extra button was the one visible difference from the
 * reference.</p>
 *
 * <p><b>Membership is derived, not listed.</b> The reference keeps a precomputed
 * item list per section. This does the same job from the predicate the mod
 * already has ({@link FloraAdvancements#isFloraDrink}), so a new drink or cake
 * cannot fall out of step with the menu.</p>
 *
 * <p><b>Icons are existing items, not new art</b> - the reference column draws a
 * representative item too.</p>
 */
public enum FloraTabCategory {

    /**
     * The 26 flower drinks - anything Cookery renders as a teacup.
     *
     * <p>Icon chosen by the author: <b>Lullaby (安眠曲)</b>. It was briefly a flower
     * cake, which was the wrong idea - a cake is not a drink, and the button is
     * the only thing telling the player what the section holds.</p>
     */
    DRINKS("drinks", () -> new ItemStack(TeacupRegistry.getItem(
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "lullaby")))),

    /** Tea bags, the mooncake and the flower cakes. */
    OTHER("other", () -> new ItemStack(BlossomMooncakes.ITEM.get()));

    private final String key;
    private final Supplier<ItemStack> icon;

    FloraTabCategory(String key, Supplier<ItemStack> icon) {
        this.key = key;
        this.icon = icon;
    }

    /** The item drawn on the button. */
    public ItemStack icon() {
        return icon.get();
    }

    /** Translated name, also used as the hover tooltip. */
    public Component label() {
        return Component.translatable("tooltip.kaleidoscope_flora.tab." + key);
    }

    /** Whether an item belongs in this section. */
    public boolean accepts(ItemStack stack) {
        // Strictly exclusive: a drink shows up in DRINKS only, everything else in
        // OTHER only. (A non-exclusive version made drinks appear in two sections
        // at once.)
        if (this == DRINKS) {
            return FloraAdvancements.isFloraDrink(stack);
        }
        return !FloraAdvancements.isFloraDrink(stack);
    }
}
