package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.effect.ModEffects;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class VampiricHeartFlash {

    private static float lastHealth = Float.NaN;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer self)) {
            return;
        }
        float health = self.getHealth();
        if (!Float.isNaN(lastHealth)
                && health > lastHealth
                && self.hasEffect(ModEffects.VAMPIRIC)) {
            Gui gui = Minecraft.getInstance().gui;
            int gained = Math.max(1, Mth.ceil(health) - Mth.ceil(lastHealth));
            gui.healthBlinkTime = gui.tickCount + 10L;
            gui.lastHealthTime = Util.getMillis();
            gui.displayHealth = Math.min(20, Mth.ceil(health) + gained);
        }
        lastHealth = health;
    }

    private VampiricHeartFlash() {
    }
}
