package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.mojang.logging.LogUtils;
import com.rlosking.flora.soupbase.LegacyMilkSoupBase;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Kaleidoscope Flora - an addon for Kaleidoscope Cookery.
 *
 * <p>Almost all drink content (drink items, teacup blocks, creative tab
 * entries) is registered automatically by Kaleidoscope Cookery for every
 * entry we push into its public {@code TeacupRegistry.TEACUP_DATA_MAP} - see
 * {@link FloraDrinks}. The only code of our own is the custom effect layer
 * ({@link ModEffects}) and the event handlers that realise the mechanics
 * ({@link FloraEvents}).</p>
 */
@Mod(KaleidoscopeFlora.MOD_ID)
public class KaleidoscopeFlora {
    public static final String MOD_ID = "kaleidoscope_flora";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Legacy soup base id for milk, stored by stockpots in worlds saved with Flora 0.3.1 and earlier. */
    public static final ResourceLocation MILK_SOUP_BASE =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "milk");

    public KaleidoscopeFlora(IEventBus modBus, ModContainer container) {
        // Register the COMMON config so server admins can tune gameplay knobs
        // (effect durations, aura ranges, combat percentages) without code edits.
        container.registerConfig(ModConfig.Type.COMMON, FloraConfig.SPEC);
        // Custom MobEffects must go through the vanilla MOB_EFFECT registry,
        // which only accepts registrations during the mod bus register phase.
        ModEffects.register(modBus);

        // Advancement criteria triggers (vanilla TRIGGER_TYPE registry) and
        // the tasted-drinks data attachment; same mod-bus window as above.
        FloraAdvancements.register(modBus);

        // Teacup data must be pushed inside the mod constructor: NeoForge
        // constructs ALL mods first, and only afterwards fires RegisterEvent.
        // Cookery's registry event handlers then iterate the whole
        // TEACUP_DATA_MAP - including the entries we add here - and
        // auto-register blocks/items for them. Our constructor is guaranteed
        // to run after Cookery's because neoforge.mods.toml declares
        // kaleidoscope_cookery with ordering = "AFTER" and type = "required".
        FloraDrinks.registerAll();

        // Since 0.3.2 the Hanami Tale recipe uses Cookery 1.5.0's native
        // minecraft:milk. Worlds saved by 0.3.1 may still hold
        // kaleidoscope_flora:milk inside a stockpot; alias that id to the
        // native milk base so those pots keep rendering and can be scooped
        // out. Registered unconditionally here: the alias resolves its
        // delegate lazily, so it does not depend on Cookery's own
        // registration timing (Cookery registers its bases during
        // FMLCommonSetupEvent, after all mod constructors).
        SoupBaseManager.registerSoupBase(new LegacyMilkSoupBase());
        LOGGER.info("Legacy milk soup base registered as an alias for Cookery's minecraft:milk");
    }
}
