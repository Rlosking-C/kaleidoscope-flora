package com.rlosking.flora;

import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This mod's own creative tab, holding the tea bags, the mooncake and every
 * flower drink - separate from Cookery's food tab (user decision 2026-09-19).
 *
 * <p><b>How the drinks leave Cookery's tab:</b> Cookery fills its food tab
 * by iterating {@code TeacupRegistry.TEACUP_DATA_MAP.keySet()} at display
 * time, and that map's only other runtime reader is the registration pass
 * (drink effects are cached inside each TeacupItem at construction). So
 * once registration is over, removing this mod's entries from the map in
 * common setup cleanly removes the drinks from Cookery's tab without
 * touching any behaviour - and the map stays untouched for every other
 * addon.</p>
 */
public final class FloraCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KaleidoscopeFlora.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLORA_TAB =
            TABS.register("flora", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kaleidoscope_flora.flora"))
                    .icon(() -> new ItemStack(TeacupRegistry.getItem(
                            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "when_the_wind_rises"))))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "cookery_main"))
                    .displayItems(FloraCreativeTab::fill)
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
        // FMLCommonSetupEvent fires on the MOD bus, not the game bus.
        modBus.addListener(FloraCreativeTab::removeFromCookeryTab);
    }

    private static void fill(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        // The chain reads top to bottom: the tea bags, then the mooncake,
        // then the flower cakes, then every drink they turn into (see
        // FloraTeas / FlowerCakes).
        for (var item : FloraTeas.teaItems()) {
            output.accept(item.get());
        }
        output.accept(BlossomMooncakes.ITEM.get());
        for (var item : FlowerCakes.cakeItems()) {
            output.accept(item.get());
        }
        // Registry iteration follows registration order, which is the
        // catalogue order of FloraDrinks; VB drinks appear only when their
        // mod is loaded, exactly as registered.
        for (Item item : BuiltInRegistries.ITEM) {
            if (FloraAdvancements.isFloraDrink(new ItemStack(item))) {
                output.accept(item);
            }
        }
    }

    /** Removes this mod's drinks from Cookery's teacup map (see class doc). */
    private static void removeFromCookeryTab(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            int before = TeacupRegistry.TEACUP_DATA_MAP.size();
            TeacupRegistry.TEACUP_DATA_MAP.keySet()
                    .removeIf(id -> id.getNamespace().equals(KaleidoscopeFlora.MOD_ID));
            KaleidoscopeFlora.LOGGER.info("Moved {} drinks out of Cookery's food tab into the Flora tab",
                    before - TeacupRegistry.TEACUP_DATA_MAP.size());
        });
    }

    private FloraCreativeTab() {
    }
}
