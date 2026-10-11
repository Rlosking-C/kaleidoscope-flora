package com.rlosking.flora.client.creativetab;

import com.rlosking.flora.FloraCreativeTab;
import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class FloraTabSidebar {

    private static final List<FloraSectionButton> BUTTONS = new ArrayList<>();

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        BUTTONS.clear();
        int x = screen.getGuiLeft() + FloraCreativeFilter.COLUMN_X;
        int y = screen.getGuiTop() + FloraCreativeFilter.COLUMN_Y;
        for (FloraTabCategory category : FloraTabCategory.values()) {
            FloraSectionButton button = new FloraSectionButton(x, y, category);
            button.visible = false;
            BUTTONS.add(button);
            event.addListener(button);
            y += FloraCreativeFilter.BUTTON_PITCH;
        }
        refreshVisibility(screen);
    }

    @SubscribeEvent
    public static void onContainerRender(ContainerScreenEvent.Render.Background event) {
        if (event.getContainerScreen() instanceof CreativeModeInventoryScreen screen) {
            refreshVisibility(screen);
        }
    }

    public static void refreshVisibility(CreativeModeInventoryScreen screen) {
        if (BUTTONS.isEmpty()) {
            return;
        }
        boolean ours = FloraCreativeFilter.isFloraTabSelected(screen);
        for (FloraSectionButton button : BUTTONS) {
            button.visible = ours;
        }
    }

    private FloraTabSidebar() {
    }
}
