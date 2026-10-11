package com.rlosking.flora.event;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.init.FloraAdvancements;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.rlosking.flora.KaleidoscopeFlora;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class VisualEvents {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!FloraAdvancements.isFloraDrink(event.getItemStack())
                && !FloraAdvancements.isFloraCake(event.getItemStack())) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        List<Component> tooltip = event.getToolTip();

        Component description = Component.translatable(
                "tooltip." + KaleidoscopeFlora.MOD_ID + "." + id.getPath() + ".desc")
                .withStyle(ChatFormatting.GRAY);
        int insertAt = 1;
        for (int i = 1; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().isBlank()) {
                insertAt = i;
                break;
            }
        }
        tooltip.add(Math.min(insertAt, tooltip.size()), description);

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getContents() instanceof TranslatableContents contents
                    && INSTANT_EFFECT_KEYS.contains(contents.getKey())) {
                tooltip.set(i, line.copy().append(
                        Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + ".instant")));
            }
        }

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("item_group.kaleidoscope_cookery.cookery_food.name")) {
                tooltip.set(i, Component.translatable("tooltip." + KaleidoscopeFlora.MOD_ID + ".group")
                        .setStyle(line.getStyle()));
                break;
            }
        }
    }

    static void orbitParticles(ServerLevel server, LivingEntity entity, ParticleOptions particle) {
        long time = server.getGameTime();
        for (int i = 0; i < 2; i++) {
            double[] pos = orbitPos(entity, time, i);
            server.sendParticles(particle, pos[0], pos[1], pos[2], 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    static double[] orbitPos(LivingEntity entity, long time, int i) {
        double angle = time / 10.0 + i * Math.PI;
        return new double[]{
                entity.getX() + Math.cos(angle) * 0.9,
                entity.getY() + 1.0 + Math.sin(time / 12.0 + i * 2.0) * 0.2,
                entity.getZ() + Math.sin(angle) * 0.9};
    }

    private static final Set<String> INSTANT_EFFECT_KEYS = Set.of(
            "effect.kaleidoscope_flora.purge",
            "effect.kaleidoscope_flora.fadeaway",
            "effect.kaleidoscope_flora.wish");

    private VisualEvents() {
    }
}
