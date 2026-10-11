package com.rlosking.flora.event;

import com.rlosking.flora.init.BlossomMooncakes;
import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.init.ModEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import com.rlosking.flora.KaleidoscopeFlora;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.rlosking.flora.effect.FlowerPathEffect;
import com.rlosking.flora.effect.GazeEffect;
import com.rlosking.flora.effect.PetalWalkEffect;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class DrinkEvents {

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof ServerPlayer player && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.drinkFinished(player, event.getItem());
        }

        if (entity instanceof ServerPlayer player
                && player.level().getDayTime() % DAY_TICKS >= NIGHT_START
                && isLullaby(event.getItem())) {
            ItemStack cake = new ItemStack(BlossomMooncakes.ITEM.get());
            if (!player.getInventory().add(cake)) {
                player.drop(cake, false);
            }
        }

        if (!entity.hasEffect(ModEffects.TASTEBLOOM) || !(entity instanceof Player eater)) {
            return;
        }
        FoodProperties food = event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
        if (food != null) {
            eater.getFoodData().eat(Math.round(food.nutrition() * 0.2f), food.saturation() * 0.2f);
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player
                && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.sipStarted(player);
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance applying = event.getEffectInstance();
        if (applying == null
                || !event.getEntity().hasEffect(ModEffects.ABSOLVE)
                || applying.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        LivingEntity warded = event.getEntity();
        UUID uuid = warded.getUUID();
        int left = ABSOLVE_CHARGES_LEFT.getOrDefault(uuid, ABSOLVE_CHARGES) - 1;
        if (left <= 0) {
            ABSOLVE_CHARGES_LEFT.remove(uuid);
            warded.removeEffect(ModEffects.ABSOLVE);
            if (warded.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD,
                        warded.getX(), warded.getY() + 1.0, warded.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
            }
        } else {
            ABSOLVE_CHARGES_LEFT.put(uuid, left);
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModEffects.DROWSY) && player.level().getDayTime() % 24000L < 2400L) {
            FloraAdvancements.award(player, FloraAdvancements.EVENT_LULLABY_DAWN);
        }
    }

    private static boolean isLullaby(ItemStack stack) {
        return FloraAdvancements.isFloraDrink(stack)
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("lullaby");
    }

    static void shortenEffect(LivingEntity target, Holder<MobEffect> effect, int costTicks) {
        MobEffectInstance instance = target.getEffect(effect);
        if (instance == null) {
            return;
        }
        int remaining = instance.getDuration() - costTicks;
        target.removeEffect(effect);
        if (remaining > 0) {
            target.addEffect(new MobEffectInstance(instance.getEffect(), remaining,
                    instance.getAmplifier(), instance.isAmbient(), instance.isVisible()));
        }
    }

    static boolean hasWarmth(LivingEntity entity) {
        if (warmthEffect == null) {
            warmthEffect = BuiltInRegistries.MOB_EFFECT.getHolder(
                    ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "warmth")).orElse(null);
        }
        return warmthEffect != null && entity.hasEffect(warmthEffect);
    }

    private static final long DAY_TICKS = 24000L;

    private static final long NIGHT_START = 12000L;

    private static final Map<UUID, Integer> ABSOLVE_CHARGES_LEFT = new HashMap<>();

    private static final int ABSOLVE_CHARGES = 3;

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        ABSOLVE_CHARGES_LEFT.remove(uuid);
        FlowerPathEffect.clearPlayer(uuid);
        GazeEffect.clearPlayer(uuid);
        PetalWalkEffect.clearPlayer(uuid);
    }

    static Holder<MobEffect> warmthEffect;

    private DrinkEvents() {
    }
}
