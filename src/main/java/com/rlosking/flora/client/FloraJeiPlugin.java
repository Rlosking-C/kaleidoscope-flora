package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/**
 * JEI integration for Kaleidoscope Flora flower drinks.
 *
 * <p>Cookery's own JEI plugin (ModJeiPlugin) already registers a
 * StockpotRecipeCategory that shows every stockpot recipe - including
 * all 26 of our flower drinks - with their ingredients. What it does
 * <b>not</b> show is each drink's mechanical description and its maxim,
 * because those are addon-specific flavour text that lives in our lang
 * files, not Cookery's recipe data.</p>
 *
 * <p>This plugin fills that gap: it registers an info page for every
 * flower drink item, so players browsing JEI can press [u] / right-click
 * "Information" and read the mechanical description and the poetic maxim
 * in one place - plus the maxim's source attribution (each maxim draws
 * on a different world civilization's poetry, proverbs or myth - the
 * 26 quotes span 26 civilizations with zero repeats). The source line is
 * deliberately JEI-only: the inventory tooltip keeps just the bare maxim,
 * and the deeper provenance waits for players who go looking for it.</p>
 *
 * <p>Soft dependency: the class is annotated with {@link JeiPlugin}
 * and loaded by JEI's own service loader only when JEI is present.
 * If JEI is not installed this file is never touched, and the mod
 * runs fine without it.</p>
 */
@JeiPlugin
public final class FloraJeiPlugin implements IModPlugin {

    /**
     * All 26 flower drink ids, in the same order as FloraDrinks.registerAll.
     * The four VanillaBackport drinks are listed last; if the backport is
     * absent their items simply won't exist in the registry and we skip them.
     */
    private static final List<String> DRINK_IDS = List.of(
            "when_the_wind_rises",
            "lullaby",
            "first_bloom",
            "fire_waltz",
            "the_unnoticed",
            "crimson_heartbeat",
            "autumn_serenade",
            "absolution",
            "rosy_stride",
            "loves_me_not",
            "prussian_leap",
            "may_kiss",
            "fleurs_du_mal",
            "breath_of_ancients",
            "the_sunward",
            "spring_waltz",
            "tender_thorns",
            "coronation",
            "voracious_urn",
            "hanami_tale",
            "echo_of_the_end",
            "vernal_awakening",
            // VanillaBackport-gated drinks (last 4)
            "the_gaze",
            "as_you_wish",
            "springtime_stroll",
            "fleeting_bloom"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        int registered = 0;
        for (String id : DRINK_IDS) {
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, id);
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) {
                continue; // e.g. VanillaBackport drinks when the backport is absent
            }
            ItemStack stack = new ItemStack(item);
            // JEI's info page draws unstyled text in plain black (DrawableWrappedText
            // uses color 0xFF000000 with no shadow) on a light-gray panel, so
            // ChatFormatting.GRAY here would be nearly invisible. The mechanical
            // description keeps the default black for maximum contrast; the maxim
            // uses italic dark purple - the classic "flavor text" look; the maxim's
            // source attribution uses italic dark gray - the classic "quote
            // attribution" look, still readable on the light background.
            Component maxim = Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + "." + id + ".maxim")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE);
            Component source = Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + "." + id + ".source")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
            Component desc = Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + "." + id + ".desc");
            registration.addItemStackInfo(stack, maxim, source, Component.literal(" "), desc);
            registered++;
        }
        KaleidoscopeFlora.LOGGER.info("Registered JEI info pages for {} flower drinks.", registered);
    }
}
