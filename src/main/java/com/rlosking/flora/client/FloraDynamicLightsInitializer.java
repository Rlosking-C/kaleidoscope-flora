package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.init.ModEffects;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public final class FloraDynamicLightsInitializer implements DynamicLightsInitializer {
    private static final int SUNWARD_LUMINANCE = 15;

    public void onInitializeDynamicLights(DynamicLightsContext context) {
        context.entityLightSourceManager().onRegisterEvent().register(registry ->
                registry.register(EntityType.PLAYER, SunwardLuminance.INSTANCE));
        KaleidoscopeFlora.LOGGER.info(
                "Registered The Sunward dynamic light for LambDynamicLights (level {}).", SUNWARD_LUMINANCE);
    }

    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
    }

    public void onInitializeDynamicLights() {
    }

    static final class SunwardLuminance implements EntityLuminance {
        static final SunwardLuminance INSTANCE = new SunwardLuminance();

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
            if (entity instanceof Player player
                    && player.hasEffect(ModEffects.SUNWARD)
                    && !player.level().isDay()) {
                return SUNWARD_LUMINANCE;
            }
            return 0;
        }
    }
}
