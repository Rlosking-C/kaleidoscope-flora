package com.rlosking.flora.client.creativetab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One section button in the flora tab's side column.
 *
 * <p><b>Geometry copied from the reference mod's column, deliberately.</b> The
 * author asked for that column reproduced as closely as possible, so the size
 * (32x26), the placement and the icon inset (+8, +5) follow it. See
 * {@link FloraCreativeFilter} for the placement constants.</p>
 *
 * <p><b>The background is this mod's own sprite pair</b>, drawn by the artist:
 * {@code filter_tab_selected.png} and {@code filter_tab_unselected.png}, 32x26,
 * same shape, differing only in their fill colour - the same construction the
 * reference mod uses. Nothing third-party ships, so this mod's MIT licence stays
 * accurate; see CREDITS.md.</p>
 *
 * <p>The reference's own two sprites are <b>not</b> used: they belong to a mod
 * whose assets are CC BY-NC-ND 4.0, which permits verbatim copies but forbids
 * derivatives, and taking them would have made this package non-commercial.
 * They were studied, measured and then redrawn with this mod's own pink/plum
 * bevel; the measurements are recorded in
 * {@code docs/策划文档/美术交付/参考图/世界名酒侧栏/}.</p>
 *
 * <p>Two earlier attempts are worth not repeating. Blitting vanilla's button via
 * {@code super.renderWidget} and then washing the whole button white to mark the
 * selection flattened the bevel - reported as "the layer is wrong, and it should
 * match vanilla's frame colours". Drawing the bevel by hand in vanilla's palette
 * fixed the shading but was still only an approximation. A real sprite pair is
 * the right answer, and the two files are the only thing that has to change for
 * a future re-skin.</p>
 *
 * <p><b>Attribution.</b> The layout, the two-section model and the
 * select-then-refresh flow follow {@code CreativeTabFilter} from Kaleidoscope
 * World Liquor (MIT), authored by "111". See CREDITS.md.</p>
 */
public class FloraSectionButton extends Button {

    /** Sprite ids, resolved to assets/kaleidoscope_flora/textures/gui/sprites/. */
    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_flora", "filter_tab_selected");
    private static final ResourceLocation TAB_UNSELECTED =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_flora", "filter_tab_unselected");

    /** Hover feedback, kept subtle so it cannot be mistaken for the selected fill. */
    private static final int HOVER_TINT = 0x20FFFFFF;

    private final FloraTabCategory category;

    public FloraSectionButton(int x, int y, FloraTabCategory category) {
        super(x, y, FloraCreativeFilter.BUTTON_WIDTH, FloraCreativeFilter.BUTTON_HEIGHT,
                Component.empty(), button -> select(category), DEFAULT_NARRATION);
        this.category = category;
        setTooltip(Tooltip.create(category.label()));
    }

    private static void select(FloraTabCategory category) {
        FloraCreativeFilter.setCurrent(category);
        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen) {
            FloraCreativeFilter.apply(screen);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The sprite carries the whole look, selection included: the two files are
        // the same shape and differ only in their fill colour. Nothing is drawn
        // over them except the icon and a hover tint, so the frame keeps its own
        // shading - an earlier build washed the whole button white instead, which
        // flattened the bevel.
        graphics.blitSprite(
                FloraCreativeFilter.current() == category ? TAB_SELECTED : TAB_UNSELECTED,
                getX(), getY(), getWidth(), getHeight());
        if (isHoveredOrFocused()) {
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1,
                    getY() + getHeight() - 1, HOVER_TINT);
        }
        ItemStack icon = category.icon();
        graphics.renderItem(icon, getX() + 8, getY() + 5);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        // Button narrates its message, which is empty here on purpose.
        output.add(NarratedElementType.TITLE, category.label());
    }
}
