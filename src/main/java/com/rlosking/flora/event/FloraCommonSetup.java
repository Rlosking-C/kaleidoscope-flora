package com.rlosking.flora.event;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.rlosking.flora.init.FlowerCakes;
import com.rlosking.flora.KaleidoscopeFlora;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraCommonSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DispenserBlock.registerProjectileBehavior(FlowerCakes.RAW.get()));
    }

    private FloraCommonSetup() {
    }
}
