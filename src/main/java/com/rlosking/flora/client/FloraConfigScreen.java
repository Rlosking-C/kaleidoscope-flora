package com.rlosking.flora.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Mod-list "Configure" button support.
 *
 * <p>NeoForge ships a generic editor for {@code ModConfigSpec} files; without
 * an {@link IConfigScreenFactory} extension point the button stays greyed
 * out. We hand it the built-in screen instead of hand-rolling a GUI, so the
 * double-language comments in {@code FloraConfig} come through for free.</p>
 *
 * <p>Client-only: kept in its own class so the common mod class never
 * references a client-only type on a dedicated server.</p>
 */
public final class FloraConfigScreen {

    public static void register(ModContainer container) {
        // Typed local (not an inline lambda): ModContainer has both a T and a
        // Supplier<T> overload, and a bare lambda is ambiguous between them.
        IConfigScreenFactory factory = ConfigurationScreen::new;
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }

    private FloraConfigScreen() {
    }
}
