package com.rlosking.flora;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Post-registration wiring that is not a registry entry of our own.
 *
 * <p><b>Why the raw cake needs this at all.</b> {@link RawFlowerCakeItem}
 * implements {@code ProjectileItem}, which is what lets a dispenser fire it -
 * but implementing the interface is <b>not sufficient</b>, and the first version
 * of this mod assumed it was. Testing found the result: "the dispenser throws it
 * out rather than firing it, it comes out as a dropped item".</p>
 *
 * <p>The reason is that vanilla dispatches dispensing through a plain
 * {@code Map<Item, DispenseItemBehavior>} and registers projectile behaviour
 * <b>item by item</b>, not by interface:</p>
 *
 * <pre>{@code
 * // DispenseItemBehavior.bootStrap()
 * DispenserBlock.registerProjectileBehavior(Items.SNOWBALL);
 * }</pre>
 *
 * <p>There is no "any {@code ProjectileItem}" sweep in that bootstrap - so a mod
 * item is simply absent from the table and falls through to the default
 * behaviour, which ejects the stack as an item entity. The fix is the same call
 * the snowball gets.</p>
 *
 * <p><b>Why {@code FMLCommonSetupEvent} and not the mod constructor.</b> The
 * call has to land in {@code DispenserBlock}'s static map <em>after</em> vanilla
 * has filled it - {@code bootStrap()} populates the snowball and friends during
 * that class's static initialisation. Touching {@code DispenserBlock} from a mod
 * constructor would force that initialisation at an awkward moment; common setup
 * is the phase intended for this kind of "adjust a vanilla table once" work, and
 * it runs after registries are complete.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraCommonSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // enqueueWork: the event fires on the loading thread, and DISPENSER_REGISTRY
        // is a plain HashMap read from the game thread.
        event.enqueueWork(() -> DispenserBlock.registerProjectileBehavior(FlowerCakes.RAW.get()));
    }

    private FloraCommonSetup() {
    }
}
