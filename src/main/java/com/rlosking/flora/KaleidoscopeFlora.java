package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.mojang.logging.LogUtils;
import com.rlosking.flora.compat.LegacyMilkSoupBase;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.rlosking.flora.init.ModEffects;
import com.rlosking.flora.init.BlossomMooncakes;
import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.init.FloraCreativeTab;
import com.rlosking.flora.init.FloraDrinks;
import com.rlosking.flora.init.FloraTeas;
import com.rlosking.flora.init.FlowerCakes;
import com.rlosking.flora.client.FloraConfigScreen;

@Mod(KaleidoscopeFlora.MOD_ID)
public class KaleidoscopeFlora {
    public static final String MOD_ID = "kaleidoscope_flora";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation MILK_SOUP_BASE =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "milk");

    public KaleidoscopeFlora(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, FloraConfig.SPEC);
        ModEffects.register(modBus);

        FloraAdvancements.register(modBus);

        FloraTeas.register(modBus);

        BlossomMooncakes.register(modBus);

        FlowerCakes.register(modBus);

        FloraCreativeTab.register(modBus);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            com.rlosking.flora.client.FloraConfigScreen.register(container);
        }

        FloraDrinks.registerAll();

        SoupBaseManager.registerSoupBase(new LegacyMilkSoupBase());
        LOGGER.info("Legacy milk soup base registered as a save-compat alias for Cookery's minecraft:milk");
    }
}
