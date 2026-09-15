package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.ModEffects;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/**
 * Sunflower "The Sunward" night light, powered by LambDynamicLights v4.
 *
 * <p>Soft dependency: none of our own code references this class. LambDynamicLights
 * discovers and instantiates it through the {@code lambdynlights:initializer}
 * entrypoint declared under {@code modproperties} in {@code neoforge.mods.toml},
 * so it is only ever loaded when that mod is installed on the client.</p>
 *
 * <p>This class implements all three historical shapes of
 * {@code onInitializeDynamicLights} instead of using {@code @Override}:
 * SodiumDynamicLights' compat jar ships an older
 * {@code DynamicLightsInitializer} under the same package, and which copy
 * javac resolves depends on classpath order. Providing every overload keeps
 * the compiled class valid against either interface version.</p>
 */
public final class FloraDynamicLightsInitializer implements DynamicLightsInitializer {
    /**
     * Sunward dynamic light level - full brightness, the same as a light
     * block or glowstone: it is the sunflower keeping the day alive at night.
     */
    private static final int SUNWARD_LUMINANCE = 15;

    /** The overload LambDynamicLights v4 actually invokes. */
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        context.entityLightSourceManager().onRegisterEvent().register(registry ->
                registry.register(EntityType.PLAYER, SunwardLuminance.INSTANCE));
        KaleidoscopeFlora.LOGGER.info(
                "Registered The Sunward dynamic light for LambDynamicLights (level {}).", SUNWARD_LUMINANCE);
    }

    /** Legacy v4 overload the API keeps abstract; superseded by the context version above. */
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
    }

    /** Old SodiumDynamicLights compat shape; its registration path is handled by
     * {@link DynamicLightIntegration} instead, so this stays a no-op. */
    public void onInitializeDynamicLights() {
    }

    /**
     * Lights the player up while the Sunward effect holds at night. Additive
     * with LambDynamicLights' own sources: the engine takes the max luminance
     * across every matching light source, so held torches still glow.
     */
    static final class SunwardLuminance implements EntityLuminance {
        static final SunwardLuminance INSTANCE = new SunwardLuminance();

        /** Registered type id, also usable from entity lighting JSON in resource packs. */
        static final EntityLuminance.Type TYPE = EntityLuminance.Type.registerSimple(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "sunward"), INSTANCE);

        private SunwardLuminance() {
        }

        @Override
        public EntityLuminance.Type type() {
            return TYPE;
        }

        @Override
        public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
            // The flower glows for the extinguished sun: light up at night
            // while the sunward effect holds, matching vanilla's "glowing"
            // visual that the effect already applies.
            if (entity instanceof Player player
                    && player.hasEffect(ModEffects.SUNWARD)
                    && !player.level().isDay()) {
                return SUNWARD_LUMINANCE;
            }
            return 0;
        }
    }
}
