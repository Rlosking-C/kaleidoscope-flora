package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.init.ModEffects;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class DynamicLightIntegration {

    private static final int SUNWARD_LUMINANCE = 15;

    private DynamicLightIntegration() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        boolean registered = false;

        if (ModList.get().isLoaded("sodiumdynamiclights")) {
            registered |= tryRegister("SodiumDynamicLights", DynamicLightIntegration::registerPlayerLight);
        }
        if (ModList.get().isLoaded("lambdynlights")
                && !classPresent("dev.lambdaurora.lambdynlights.api.DynamicLightsContext")) {
            registered |= tryRegister("LambDynamicLights", DynamicLightIntegration::registerPlayerLight);
        }
        if (ModList.get().isLoaded("ryoamiclights")) {
            registered |= tryRegister("RyoamicLights", DynamicLightIntegration::registerRyoamicLight);
        }

        if (!registered) {
            KaleidoscopeFlora.LOGGER.info(
                    "No dynamic-lighting engine found - The Sunward will only glow via its outline.");
        }
    }

    private static boolean tryRegister(String engineName, Runnable registration) {
        try {
            registration.run();
            KaleidoscopeFlora.LOGGER.info(
                    "Registered The Sunward dynamic light for {} (level {}).", engineName, SUNWARD_LUMINANCE);
            return true;
        } catch (Throwable t) {
            KaleidoscopeFlora.LOGGER.warn("{} integration failed: {}", engineName, t.toString());
            return false;
        }
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, DynamicLightIntegration.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void registerPlayerLight() {
        DynamicLightHandlers.registerDynamicLightHandler(EntityType.PLAYER,
                DynamicLightHandler.makeHandler(
                        player -> {
                            if (player.hasEffect(ModEffects.SUNWARD) && !player.level().isDay()) {
                                return SUNWARD_LUMINANCE;
                            }
                            return 0;
                        },
                        player -> false));
        assert Minecraft.getInstance() != null;
    }

    private static void registerRyoamicLight() {
        org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers.registerDynamicLightHandler(EntityType.PLAYER,
                org.thinkingstudio.ryoamiclights.api.DynamicLightHandler.makeHandler(
                        player -> {
                            if (player.hasEffect(ModEffects.SUNWARD) && !player.level().isDay()) {
                                return SUNWARD_LUMINANCE;
                            }
                            return 0;
                        },
                        player -> false));
    }
}
