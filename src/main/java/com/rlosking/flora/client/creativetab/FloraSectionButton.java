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

public class FloraSectionButton extends Button {

    private static final ResourceLocation TAB_SELECTED =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_flora", "filter_tab_selected");
    private static final ResourceLocation TAB_UNSELECTED =
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_flora", "filter_tab_unselected");

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
        output.add(NarratedElementType.TITLE, category.label());
    }
}
