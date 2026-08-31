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
 * Sunflower "The Sunward" night light, powered by SodiumDynamicLights.
 *
 * <p>Soft dependency: the integration only touches that mod's API inside
 * {@link #registerPlayerLight()}, which is never invoked unless
 * sodiumdynamiclights is installed. The JVM resolves method-body class
 * references lazily, so the game boots fine without the mod present.</p>
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
        if (!ModList.get().isLoaded("sodiumdynamiclights")) {
            KaleidoscopeFlora.LOGGER.info(
                    "SodiumDynamicLights not installed - The Sunward will only glow via its outline.");
            return;
        }
        try {
            registerPlayerLight();
            KaleidoscopeFlora.LOGGER.info("Registered The Sunward dynamic light (level {}).", SUNWARD_LUMINANCE);
        } catch (Throwable t) {
            // API drift between SDL versions must never crash the client.
            KaleidoscopeFlora.LOGGER.warn("SodiumDynamicLights integration failed: {}", t.toString());
        }
    }

    /**
     * All SodiumDynamicLights references live here on purpose - this method is
     * only linked when the mod is present, keeping the class loadable without it.
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
}
