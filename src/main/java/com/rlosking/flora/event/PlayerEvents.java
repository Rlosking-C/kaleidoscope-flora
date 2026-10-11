package com.rlosking.flora.event;

import com.rlosking.flora.compat.PatchouliGuide;
import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.init.ModEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import com.rlosking.flora.KaleidoscopeFlora;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.rlosking.flora.event.CakeEvents;
import com.rlosking.flora.event.DrinkEvents;
import com.rlosking.flora.event.VisualEvents;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class PlayerEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        UUID uuid = player.getUUID();

        double[] last = LAST_XZ.get(uuid);
        double x = player.getX();
        double z = player.getZ();
        MOVE_DELTA.put(uuid, last == null ? new double[]{0.0, 0.0} : new double[]{x - last[0], z - last[1]});
        LAST_XZ.put(uuid, new double[]{x, z});

        CakeEvents.tickDewLanding(player, uuid);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel server)) {
            return;
        }
        if (DrinkEvents.hasWarmth(entity)) {
            if (entity.getTicksFrozen() > 0) {
                entity.setTicksFrozen(0);
            }
            if (entity.isOnFire()) {
                entity.setRemainingFireTicks(0);
            }
        }
        if (server.getGameTime() % 4 == 0) {
            if (entity.hasEffect(ModEffects.PETAL_VEIL)) {
                VisualEvents.orbitParticles(server, entity, ParticleTypes.CHERRY_LEAVES);
            }
        }
        if (entity.hasEffect(MobEffects.LUCK)) {
            coronationMotes(server, entity);
        } else {
            LUCK_START.remove(entity.getUUID());
        }
        if (entity.hasEffect(ModEffects.HARVEST)) {
            attractDrops(server, entity);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPetalBob(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.PETALWALK)
                || player.isSwimming()
                || player.isDeadOrDying()) {
            return;
        }
        if (player.onGround() || !isOnPetalFilm(player)) {
            return;
        }
        float stride = Math.min(0.1F, (float) player.getDeltaMovement().horizontalDistance());
        player.bob = player.oBob + (stride - player.oBob) * 0.4F;
    }

    @SubscribeEvent
    public static void onPlayerTickAdvancements(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        FloraAdvancements.tick(player);
        com.rlosking.flora.compat.PatchouliGuide.tickFlowerUnlock(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        com.rlosking.flora.compat.PatchouliGuide.giveOnce(event.getEntity());
    }

    public static boolean isOnPetalFilm(Player player) {
        BlockPos support = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        FluidState fluid = player.level().getFluidState(support);
        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }
        if (player.level().getFluidState(support.above()).is(FluidTags.WATER)) {
            return false;
        }
        double surface = support.getY() + fluid.getHeight(player.level(), support);
        double feet = player.getY();
        return feet >= surface - 0.05 && feet <= surface + 0.15;
    }

    public static double[] lastMove(Player player) {
        double[] delta = MOVE_DELTA.get(player.getUUID());
        return delta == null ? new double[]{0.0, 0.0} : delta;
    }

    public static void coronationMotes(ServerLevel server, LivingEntity entity) {
        long time = server.getGameTime();
        long started = LUCK_START.computeIfAbsent(entity.getUUID(), k -> time);
        MobEffectInstance luck = entity.getEffect(MobEffects.LUCK);
        if (luck == null) {
            return;
        }
        double fadeIn = Math.min(1.0, (time - started) / 40.0);
        double fadeOut = Math.min(1.0, luck.getDuration() / 60.0);
        double density = fadeIn * fadeOut;
        DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.0F), 0.9F);
        for (int i = 0; i < 2; i++) {
            if (entity.getRandom().nextDouble() > density * 0.5) {
                continue;
            }
            double[] pos = VisualEvents.orbitPos(entity, time, i);
            server.sendParticles(gold, pos[0], pos[1], pos[2], 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    public static void attractDrops(ServerLevel server, LivingEntity entity) {
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class,
                entity.getBoundingBox().inflate(5.0));
        if (items.isEmpty()) {
            return;
        }
        Vec3 center = new Vec3(entity.getX(), entity.getY() + 0.6, entity.getZ());
        for (ItemEntity item : items) {
            Vec3 pull = center.subtract(item.position());
            double distance = pull.length();
            if (distance < 0.3 || distance > 5.0) {
                continue;
            }
            double speed = 0.08 + 0.12 * (1.0 - distance / 5.0);
            item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(pull.normalize().scale(speed)));
        }
    }

    public static final Map<UUID, double[]> LAST_XZ = new HashMap<>();

    public static final Map<UUID, double[]> MOVE_DELTA = new HashMap<>();

    public static final Map<UUID, Long> LUCK_START = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_XZ.remove(uuid);
        MOVE_DELTA.remove(uuid);
        LUCK_START.remove(uuid);
        FloraAdvancements.clearPlayer(uuid);
    }

    private PlayerEvents() {
    }
}
