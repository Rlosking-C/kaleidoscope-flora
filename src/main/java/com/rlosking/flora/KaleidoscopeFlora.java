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

    /**
     * Legacy soup base id for milk, stored by stockpots in worlds saved with
     * Flora 0.3.1 and earlier. See {@link LegacyMilkSoupBase}: this is a save
     * compatibility alias, NOT part of the stockpot brewing route.
     */
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

        // The flower tea bags: one per drink, crafted from the flower itself
        // plus Cookery's dried tea leaves, brewed in the teapot. Plain items
        // with no behaviour - see FloraTeas.
        FloraTeas.register(modBus);

        // The 0.3.3 Mid-Autumn mooncake: a tray block that stacks up to five
        // mooncakes, edible in its item form (see BlossomMooncakes).
        BlossomMooncakes.register(modBus);

        // The 0.3.4 flower cakes: one thrown raw cake and the two things it
        // cooks into. Plain items plus two markers; the mechanics hang off
        // events and one mixin - see FlowerCakes.
        FlowerCakes.register(modBus);

        // v0.4.0: the bloom-petal particle TYPE. Particle types are a synced
        // registry, so this belongs on both sides - and it must be registered
        // before the client's RegisterParticleProvidersEvent fires, or the
        // provider lookup hits an unbound ResourceKey and the client crashes
        // (that is exactly what happened on the first 0.4.0 run).
        FloraParticleTypes.PARTICLES.register(modBus);

        // v0.4.0 Flower Perch: the empty-perch blocks/items, the 26 planted
        // perches and the pale-oak variant gated on VanillaBackport.
        FlowerPerches.register(modBus);

        // Own creative tab (tea bags, mooncake, all drinks), and the drinks
        // leave Cookery's food tab - see FloraCreativeTab for the mechanism.
        FloraCreativeTab.register(modBus);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            // Mod-list "Configure" button: NeoForge's built-in screen for our
            // COMMON config (no hand-rolled GUI to maintain).
            com.rlosking.flora.client.FloraConfigScreen.register(container);
            // v0.4.0: the bloom petal particle and its client provider. Mod bus,
            // because RegisterParticleProvidersEvent is a mod-bus event.
            com.rlosking.flora.client.AromaParticles.register(modBus);
        }

        // Teacup data must be pushed inside the mod constructor: NeoForge
        // constructs ALL mods first, and only afterwards fires RegisterEvent.
        // Cookery's registry event handlers then iterate the whole
        // TEACUP_DATA_MAP - including the entries we add here - and
        // auto-register blocks/items for them. Our constructor is guaranteed
        // to run after Cookery's because neoforge.mods.toml declares
        // kaleidoscope_cookery with ordering = "AFTER" and type = "required".
        FloraDrinks.registerAll();

        // Save compatibility, unrelated to the stockpot brewing route that was
        // deleted on 2026-09-22: worlds saved by Flora 0.3.1 and earlier can
        // still hold kaleidoscope_flora:milk as a stockpot's soup base. Cookery
        // dereferences that lookup without a null check, so without this alias
        // such a pot throws "soupBase is null" the moment it is rendered.
        // Registered unconditionally: the delegate resolves lazily, so it does
        // not depend on Cookery's own registration timing (Cookery registers
        // its bases during FMLCommonSetupEvent, after all mod constructors).
        SoupBaseManager.registerSoupBase(new LegacyMilkSoupBase());
        LOGGER.info("Legacy milk soup base registered as a save-compat alias for Cookery's minecraft:milk");
    }
}
