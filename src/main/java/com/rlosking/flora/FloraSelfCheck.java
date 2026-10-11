package com.rlosking.flora;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Startup self-check: turns silent failures into loud ones.
 *
 * <p>NeoForge discovers event handlers by scanning for the
 * {@code @EventBusSubscriber} annotation. If that annotation is ever lost -
 * a file moved, a merge gone wrong - the handlers simply never run: no
 * error, no warning, the feature is just gone. The same is true of a class
 * that is renamed out from under a mixin config or an entry point.</p>
 *
 * <p>This class names every subscriber class explicitly and verifies, at
 * startup, that each one still exists and still carries the annotation. Any
 * problem is logged at ERROR level, and in a development environment it
 * throws, so a broken build cannot pass unnoticed.</p>
 */
public final class FloraSelfCheck {

    private static final List<String> SUBSCRIBER_CLASSES = List.of(
            "com.rlosking.flora.client.DynamicLightIntegration",
            "com.rlosking.flora.client.VampiricHeartFlash",
            "com.rlosking.flora.client.creativetab.FloraTabSidebar",
            "com.rlosking.flora.event.CakeEvents",
            "com.rlosking.flora.event.CombatEvents",
            "com.rlosking.flora.event.DrinkEvents",
            "com.rlosking.flora.event.FloraCommonSetup",
            "com.rlosking.flora.event.HarvestEvents",
            "com.rlosking.flora.event.PlayerEvents",
            "com.rlosking.flora.event.VisualEvents",
            "com.rlosking.flora.network.FloraNetwork");

    private FloraSelfCheck() {
    }

    public static void verify(Logger log) {
        List<String> broken = new ArrayList<>();
        for (String name : SUBSCRIBER_CLASSES) {
            try {
                Class<?> type = Class.forName(name);
                if (!type.isAnnotationPresent(EventBusSubscriber.class)) {
                    broken.add(name + " - missing @EventBusSubscriber");
                }
            } catch (ClassNotFoundException e) {
                broken.add(name + " - class not found");
            }
        }
        if (broken.isEmpty()) {
            log.info("[self-check] {} event classes verified", SUBSCRIBER_CLASSES.size());
            return;
        }
        log.error("[self-check] SILENT FAILURE RISK - {} event class(es) would never run: {}",
                broken.size(), broken);
        if (!FMLEnvironment.production) {
            throw new IllegalStateException("Flora self-check failed: " + broken);
        }
    }
}
