package com.rlosking.flora.client;

import com.rlosking.flora.FlowerCakes;
import com.rlosking.flora.KaleidoscopeFlora;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The item-model predicate that lets a flower cake change appearance with its
 * stack size.
 *
 * <p><b>Why this Java exists at all.</b> The design asks for a 1-to-5 stacked
 * look per cake (twelve extra models). The obvious way to switch models by stack
 * count is a {@code count} predicate in the model JSON - and <b>1.21.1 does not
 * have one</b>. Dumping {@code ItemProperties}' registration table gives the
 * complete list of predicates available on this version:</p>
 *
 * <pre>custom_model_data, damaged, damage, lefthanded, cooldown, pull, brushing,
 * pulling, filled, time, angle, charged, firework, broken, cast, blocking,
 * throwing, level, tooting</pre>
 *
 * <p>None of them is the stack size, and {@code count} only arrives with the
 * item-model rework in 1.21.2 and later. So the count has to be published from
 * code, and that is all this class does.</p>
 *
 * <p><b>The mooncake could not be copied.</b> {@code blossom_mooncake/0..4} gets
 * its five tiers for free because it is a <em>block</em>: the tiers hang off a
 * blockstate property ({@code BlossomMooncakeBlock.STACK_COUNT}). Blockstates
 * vary models by arbitrary properties; <b>items have no such thing</b>, which is
 * why the block recipe does not transfer. (The mooncake's own item form is a
 * single flat sprite - the stacked look only ever existed in the world.)</p>
 *
 * <p><b>Why the value is the raw count rather than a 0-to-1 fraction.</b> The
 * {@code ClampedItemPropertyFunction} overload exists for vanilla's
 * normalised predicates (damage fractions and the like), but {@code overrides}
 * only ever compare {@code value >= threshold}, so there is nothing to clamp.
 * Publishing the count itself lets the model author write a threshold that reads
 * as what it means:</p>
 *
 * <pre>{@code
 * "overrides": [
 *   { "predicate": { "kaleidoscope_flora:stack_count": 2 }, "model": "..._1" },
 *   { "predicate": { "kaleidoscope_flora:stack_count": 3 }, "model": "..._2" }
 * ]
 * }</pre>
 *
 * <p>...instead of a magic 0.25. Thresholds are "this many or more", which is
 * exactly how {@code ItemOverrides} resolves them.</p>
 *
 * <p><b>Client-only, and deferred onto the client thread.</b>
 * {@code ItemProperties} is a client rendering registry that does not exist on a
 * dedicated server, so this class must never be loaded there - hence
 * {@code Dist.CLIENT} on the subscriber, the same pattern as
 * {@code DynamicLightIntegration} and {@code VampiricHeartFlash}.
 * {@code enqueueWork} is the documented requirement for touching client state
 * from the mod-bus setup event: the event fires on the loading thread, while the
 * registries it touches are owned by the client thread.</p>
 *
 * <p><b>No {@code bus = ...} on the annotation.</b> The obvious spelling is
 * {@code @EventBusSubscriber(..., bus = EventBusSubscriber.Bus.MOD, ...)}, and it
 * compiles - but on NeoForge 21.1.248 both {@code bus()} and the {@code Bus} enum
 * are marked <b>deprecated for removal</b>, and javac says so. The bus is now
 * inferred from the event type, so {@code FMLClientSetupEvent} already routes to
 * the mod bus and naming it is redundant.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class FloraItemProperties {

    /**
     * Predicate id used by the flower-cake models' {@code overrides}.
     *
     * <p>Namespaced rather than plain {@code stack_count} so it cannot collide
     * with another mod's predicate of the same name.</p>
     */
    public static final ResourceLocation STACK_COUNT =
            ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "stack_count");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemPropertyFunction count = (stack, level, entity, seed) -> stack.getCount();
            register(FlowerCakes.RAW.get(), count);
            register(FlowerCakes.TOASTED.get(), count);
            register(FlowerCakes.DEW.get(), count);
        });
    }

    private static void register(Item item, ItemPropertyFunction count) {
        ItemProperties.register(item, STACK_COUNT, count);
    }

    private FloraItemProperties() {
    }
}
