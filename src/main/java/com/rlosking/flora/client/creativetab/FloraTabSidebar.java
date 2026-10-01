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

/**
 * Puts the section column on screen, and hides it again for other tabs.
 *
 * <p><b>Real widgets, added through NeoForge, not hand-rolled hit testing.</b>
 * {@code ScreenEvent.Init.Post} accepts listeners into the screen, so the buttons
 * become ordinary children: the screen renders them, routes clicks and mouse
 * movement to them, and shows their tooltips, with no mixin and no manual
 * coordinate maths for input. That is the main reason this needs only one screen
 * mixin (to re-filter when the tab changes) instead of the reference mod's
 * approach of drawing from a {@code GuiGraphics} hook.</p>
 *
 * <p><b>Positions once, visibility every frame.</b> Positions can only change
 * when the screen is re-initialised, which re-fires {@code Init.Post} - so they
 * are set there. Visibility depends on which tab is selected, and vanilla's tab
 * buttons change that without any event of their own, so it is refreshed on each
 * frame. Both operations are trivial; only the second needs to be frequent.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class FloraTabSidebar {

    private static final List<FloraSectionButton> BUTTONS = new ArrayList<>();

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // The instanceof test comes FIRST, and the list is only ever cleared for
        // our own screen. The earlier version cleared it for every screen, so any
        // other screen initialising between the creative screen opening and a tab
        // click (the pause menu, a JEI overlay) silently emptied it - and an empty
        // list makes refreshVisibility a no-op, which is why the buttons stayed
        // hidden even after the tab changed.
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        BUTTONS.clear();
        // Measured from the panel's LEFT edge. An earlier version added
        // getXSize() here as well, a leftover from when the column sat to the
        // right of the panel; combined with a negative COLUMN_X that put the
        // buttons *inside* the panel against its right edge, which is not where
        // the reference mod puts its column and not where the design wanted it.
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

    /**
     * Keeps the column's visibility in step with the selected tab, every frame.
     *
     * <p><b>This is the container screen's own render event, not
     * {@code ScreenEvent.Render.Post}.</b> The latter is fired from inside
     * {@code Screen#render}, and {@code AbstractContainerScreen} overrides
     * {@code render} <b>without ever calling it</b> - so for this screen that
     * event never fires at all. Measured, not assumed: the first two builds of
     * this feature logged from a {@code Render.Post} handler and neither ever
     * printed a single line, while every other probe did. (The reference mod
     * subscribes to {@code ScreenEvent.Render.Post} too, but its buttons are
     * {@code Button}s created visible, so it does not depend on that handler to
     * become visible; ours did.)</p>
     *
     * <p>{@code ContainerScreenEvent.Render.Background} is fired from
     * {@code AbstractContainerScreen#render} <em>before</em> it walks
     * {@code renderables}, so a change made here is drawn in the same frame.
     * Refreshing every frame costs one field read and removes the need to reason
     * about which hook fires when - {@code selectTab} also runs before
     * {@code Init.Post} while a screen is opening, so an event-only approach kept
     * leaving the first frame wrong.</p>
     */
    @SubscribeEvent
    public static void onContainerRender(ContainerScreenEvent.Render.Background event) {
        if (event.getContainerScreen() instanceof CreativeModeInventoryScreen screen) {
            refreshVisibility(screen);
        }
    }

    /**
     * Shows the column only while this mod's tab is the one on screen.
     *
     * <p><b>Deliberately not driven by a render event.</b> The first build of
     * this feature set {@code visible} from {@code ScreenEvent.Render.Post} every
     * frame, which silently does nothing for this screen:
     * {@code AbstractContainerScreen} overrides {@code Screen#render} and
     * <b>never calls it</b>, while that event is fired from inside
     * {@code Screen#render}. The buttons were therefore created, filtered
     * correctly, and stayed invisible forever - which was the "there is no
     * sidebar" report. (NeoForge's container-screen equivalent,
     * {@code ContainerScreenEvent.Render.Foreground}, does fire; it is simply not
     * needed, because visibility only changes when the selected tab changes.)</p>
     *
     * <p>Called after the screen initialises and after every tab selection - the
     * two moments the answer can change. {@code selectTab} also runs
     * <em>before</em> {@code Init.Post} while a screen is opening, which is why
     * the state is set here too: at that first call the buttons do not exist
     * yet.</p>
     */
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
