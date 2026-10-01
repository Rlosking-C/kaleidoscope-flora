package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.ModEffects;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Sunflower "The Sunward" night light, handed to whichever dynamic-lighting
 * engine the player runs. Soft dependencies, engine by engine:
 *
 * <ul>
 *   <li><b>LambDynamicLights v4</b> (official) - registered through the
 *       {@code lambdynlights:initializer} entrypoint instead of this class;
 *       see {@link FloraDynamicLightsInitializer}.</li>
 *   <li><b>SodiumDynamicLights</b> - registered through the legacy
 *       LambDynamicLights API classes its compat jar ships.</li>
 *   <li><b>LambDynamicLights v3-era ports</b> under the same mod id
 *       ({@code lambdynlights}), e.g. the unofficial NeoForge 3.1.4 build -
 *       same legacy API, so the same registration call; told apart from v4
 *       by the absence of the {@code DynamicLightsContext} API class, which
 *       keeps official v4 on the entrypoint path instead of this one.</li>
 *   <li><b>RyoamicLights</b> - ThinkingStudios' Architectury port of
 *       LambDynamicLights with the API renamed to
 *       {@code org.thinkingstudio.ryoamiclights.api}.</li>
 * </ul>
 *
 * <p>Each engine's references are confined to one registration method, which
 * the JVM only links when that engine is present, so the game boots fine
 * with none of them installed.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class DynamicLightIntegration {

    /**
     * Sunward dynamic light level - full brightness, the same as a light
     * block or glowstone: it is the sunflower keeping the day alive at night.
     */
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
            // v3-era port under the same mod id as official v4 - only the old
            // build lacks DynamicLightsContext.
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
            // API drift between engine versions must never crash the client.
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

    /**
     * All legacy-API references live here on purpose - this method is only
     * linked when an engine shipping them is present, keeping the class
     * loadable without them.
     */
    private static void registerPlayerLight() {
        DynamicLightHandlers.registerDynamicLightHandler(EntityType.PLAYER,
                DynamicLightHandler.makeHandler(
                        player -> {
                            // The flower glows for the extinguished sun: light up at
                            // night while the sunward effect holds, matching vanilla's
                            // "glowing" visual that the effect already applies.
                            if (player.hasEffect(ModEffects.SUNWARD) && !player.level().isDay()) {
                                return SUNWARD_LUMINANCE;
                            }
                            return 0;
                        },
                        player -> false));
        // Registering for the local player only matters visually; Minecraft is
        // only referenced to prove the client environment is available here.
        assert Minecraft.getInstance() != null;
    }

    /**
     * All RyoamicLights references live here on purpose - same isolation rule
     * as {@link #registerPlayerLight()}.
     */
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
