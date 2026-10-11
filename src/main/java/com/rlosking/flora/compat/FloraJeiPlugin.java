package com.rlosking.flora.compat;

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

@JeiPlugin
public final class FloraJeiPlugin implements IModPlugin {

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
                continue;
            }
            ItemStack stack = new ItemStack(item);
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
