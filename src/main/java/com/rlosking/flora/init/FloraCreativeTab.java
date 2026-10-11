package com.rlosking.flora.init;

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
import com.rlosking.flora.KaleidoscopeFlora;

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
        modBus.addListener(FloraCreativeTab::removeFromCookeryTab);
    }

    private static void fill(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (var item : FloraTeas.teaItems()) {
            output.accept(item.get());
        }
        output.accept(BlossomMooncakes.ITEM.get());
        for (var item : FlowerCakes.cakeItems()) {
            output.accept(item.get());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (FloraAdvancements.isFloraDrink(new ItemStack(item))) {
                output.accept(item);
            }
        }
    }

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
