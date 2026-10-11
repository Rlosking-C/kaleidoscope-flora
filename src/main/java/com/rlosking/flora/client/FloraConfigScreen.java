package com.rlosking.flora.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class FloraConfigScreen {

    public static void register(ModContainer container) {
        IConfigScreenFactory factory = ConfigurationScreen::new;
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }

    private FloraConfigScreen() {
    }
}
