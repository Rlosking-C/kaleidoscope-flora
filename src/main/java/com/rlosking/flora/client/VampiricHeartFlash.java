package com.rlosking.flora.client;

import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.ModEffects;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The crimson heartbeat's missing flourish. Vanilla only blinks the heart
 * row white when health changes while {@code invulnerableTime > 0} - i.e.
 * in the middle of being hit - so a lifesteal heal granted to the ATTACKER
 * never satisfies the condition and stolen hearts fill in silently. This
 * client-only hook watches the local player's health while Vampiric is
 * active and reproduces exactly what vanilla's own branch does (see
 * Gui#renderHealthLevel): the freshly gained hearts render as blinking
 * white ghosts for ten ticks.
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID, value = Dist.CLIENT)
public final class VampiricHeartFlash {

    /** Health as seen on the previous client tick; NaN until first observed. */
    private static float lastHealth = Float.NaN;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Only the local player's HUD is ours to animate; the integrated
        // server also ticks ServerPlayers through this same JVM.
        if (!(event.getEntity() instanceof LocalPlayer self)) {
            return;
        }
        float health = self.getHealth();
        if (!Float.isNaN(lastHealth)
                && health > lastHealth
                && self.hasEffect(ModEffects.VAMPIRIC)) {
            Gui gui = Minecraft.getInstance().gui;
            // Mirror the "heal while invulnerable" branch of vanilla's
            // renderHealthLevel: blink for ten ticks, and keep displayHealth
            // above the new value so the gained hearts show as white ghosts
            // until the blink ends (the 1s resync then clears the rest).
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
