package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.mojang.logging.LogUtils;
import com.rlosking.flora.soupbase.MilkSoupBase;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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

    /** Soup base id for milk: Hanami Tale is a latte, so its base is milk, not water. */
    public static final ResourceLocation MILK_SOUP_BASE =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "milk");

    public KaleidoscopeFlora(IEventBus modBus) {
        // Custom MobEffects must go through the vanilla MOB_EFFECT registry,
        // which only accepts registrations during the mod bus register phase.
        ModEffects.register(modBus);

        // Teacup data must be pushed inside the mod constructor: NeoForge
        // constructs ALL mods first, and only afterwards fires RegisterEvent.
        // Cookery's registry event handlers then iterate the whole
        // TEACUP_DATA_MAP - including the entries we add here - and
        // auto-register blocks/items for them. Our constructor is guaranteed
        // to run after Cookery's because neoforge.mods.toml declares
        // kaleidoscope_cookery with ordering = "AFTER" and type = "required".
        FloraDrinks.registerAll();

        // Milk as a stockpot soup base for Hanami Tale (poured from a milk
        // bucket, empty bucket returned, milk-white bubbling surface). Runs
        // after Cookery's registerAll() by the same mod-construction ordering
        // guarantee documented above.
        SoupBaseManager.registerSoupBase(new MilkSoupBase());
    }
}
