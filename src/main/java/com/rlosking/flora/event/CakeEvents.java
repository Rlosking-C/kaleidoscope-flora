package com.rlosking.flora.event;

import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.init.ModEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.rlosking.flora.KaleidoscopeFlora;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class CakeEvents {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.FEATHERFALL)) {
            event.setCanceled(true);
            playSoftLanding(entity, event.getDistance());
            return;
        }
        if (entity.hasEffect(ModEffects.DEW_CAKE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCakeEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance expired = event.getEffectInstance();
        if (expired == null || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (expired.getEffect().equals(ModEffects.TOASTED_CAKE)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 0, true, true));
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        LivingEntity target = pinTarget(hit.getEntity());
        if (target == null) {
            return;
        }
        event.setCanceled(true);
        snowball.discard();
        if (target.level().isClientSide()) {
            return;
        }
        Entity source = snowball.getOwner();
        target.forceAddEffect(
                new MobEffectInstance(ModEffects.PINNED, ModEffects.PIN_TICKS, 0, true, true), source);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                ModEffects.PIN_TICKS, PIN_SLOW_AMPLIFIER, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                ModEffects.PIN_TICKS, PIN_WEAKNESS_AMPLIFIER, true, true));
        if (target.level() instanceof ServerLevel server) {
            playPinImpact(server, target);
        }
        if (snowball.getOwner() instanceof ServerPlayer shooter) {
            FloraAdvancements.rawFlowerCakePin(shooter, target.getUUID());
        }
    }

    public static void noteMidAirJump(ServerPlayer player, boolean stillRising) {
        MIDAIR_JUMP_REPORTED.merge(player.getUUID(), stillRising,
                (existing, incoming) -> existing && incoming);
    }

    private static void playSoftLanding(LivingEntity entity, float distance) {
        if (entity.isSilent() || distance <= 1.5f) {
            return;
        }
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.2F, entity.getZ());
        BlockState state = entity.level().getBlockState(below);
        if (!state.isAir()) {
            SoundType soundType = state.getSoundType(entity.level(), below, entity);
            entity.playSound(soundType.getFallSound(), soundType.getVolume() * 0.5F, soundType.getPitch() * 0.75F);
        }
    }

    static void tickDewLanding(Player player, UUID uuid) {
        if (!player.hasEffect(ModEffects.DEW_CAKE)) {
            DEW_AIR.remove(uuid);
            MIDAIR_JUMP_REPORTED.remove(uuid);
            return;
        }
        double y = player.getY();
        if (!player.onGround()) {
            AirStretch stretch = DEW_AIR.get(uuid);
            if (stretch == null) {
                DEW_AIR.put(uuid, new AirStretch(y));
            } else {
                stretch.observe(y);
            }
            Boolean reported = MIDAIR_JUMP_REPORTED.get(uuid);
            AirStretch live = DEW_AIR.get(uuid);
            if (reported != null && live != null) {
                live.midAirJump = true;
                live.spamJump = reported;
            }
            return;
        }
        MIDAIR_JUMP_REPORTED.remove(uuid);
        AirStretch stretch = DEW_AIR.remove(uuid);
        if (stretch == null) {
            return;
        }
        double drop = stretch.apex - y;
        boolean solid = DewImpact.landsOnSolidGround(player);
        boolean fired = DewImpact.settle(player, drop, stretch.midAirJump, stretch.spamJump);
    }

    private static LivingEntity pinTarget(Entity hit) {
        if (hit instanceof LivingEntity living) {
            return living;
        }
        if (hit instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) {
            return parent;
        }
        return null;
    }

    private static void playPinImpact(ServerLevel server, LivingEntity target) {
        double radius = ModEffects.pinRingRadius(target);
        int count = ModEffects.pinRingCount(radius);
        double centreY = target.getY() + target.getBbHeight() * 0.5;
        for (int i = 0; i < count; i++) {
            double angle = (i / (double) count) * Math.PI * 2.0;
            server.sendParticles(ParticleTypes.CRIT,
                    target.getX() + Math.cos(angle) * radius,
                    centreY,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0.0, 0.05, 0.0, 0.02);
        }
        server.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    static final class DewImpact {

        private static final double[][] TIERS = {
                {2.0, 1.5, 0.4, 2.0},
                {5.0, 2.5, 0.8, 4.0},
                {10.0, 3.5, 1.4, 6.0},
                {20.0, 5.0, 2.0, 10.0},
        };

        private static final Set<Block> SOFT_LANDINGS = Set.of(
                Blocks.POWDER_SNOW, Blocks.HAY_BLOCK, Blocks.HONEY_BLOCK,
                Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED,
                Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED,
                Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED,
                Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED);

        private static final double PARTICLE_RADIUS = 6.0;

        private static final double MIDAIR_JUMP_MIN_DROP = 1.6;

        private DewImpact() {
        }

        static boolean settle(Player player, double drop, boolean midAirJump, boolean spamJump) {
            if (spamJump) {
                return false;
            }
            boolean qualifies = drop >= TIERS[0][0]
                    || (midAirJump && drop >= MIDAIR_JUMP_MIN_DROP);
            if (!qualifies || !landsOnSolidGround(player)) {
                return false;
            }
            int tier = TIERS.length - 1;
            for (int i = 0; i < TIERS.length; i++) {
                if (drop < TIERS[i][0]) {
                    tier = i - 1;
                    break;
                }
            }
            tier = Math.max(0, tier);
            double radius = TIERS[tier][1];
            double knockback = TIERS[tier][2];
            double damage = TIERS[tier][3];

            List<LivingEntity> hit = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(radius), other -> other != player);
            for (LivingEntity victim : hit) {
                victim.hurt(player.damageSources().playerAttack(player), (float) damage);
                Vec3 away = victim.position().subtract(player.position());
                Vec3 flat = new Vec3(away.x, 0.0, away.z);
                Vec3 direction = flat.lengthSqr() < 1.0e-4
                        ? new Vec3(0.0, 0.0, 1.0)
                        : flat.normalize();
                victim.push(direction.x * knockback, knockback * 0.6, direction.z * knockback);
                victim.hurtMarked = true;
            }
            if (player.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.2, player.getZ(),
                        40, radius * 0.4, 0.2, radius * 0.4, 0.05);
                server.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY() + 0.4, player.getZ(),
                        30, PARTICLE_RADIUS * 0.3, 0.3, PARTICLE_RADIUS * 0.3, 0.1);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            if (!hit.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                FloraAdvancements.dewImpact(serverPlayer, drop);
            }
            return !hit.isEmpty();
        }

        static boolean landsOnSolidGround(Player player) {
            if (player.isInFluidType() || player.isInPowderSnow) {
                return false;
            }
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
            if (!player.level().getFluidState(below).isEmpty()) {
                return false;
            }
            BlockState state = player.level().getBlockState(below);
            if (state.isAir() || SOFT_LANDINGS.contains(state.getBlock())) {
                return false;
            }
            return !state.getCollisionShape(player.level(), below).isEmpty();
        }
    }

    private static final class AirStretch {

        final double takeoffY;
        double apex;

        boolean midAirJump;

        boolean spamJump;

        AirStretch(double y) {
            this.takeoffY = y;
            this.apex = y;
        }

        void observe(double y) {
            if (y > apex) {
                apex = y;
            }
        }
    }

    private static final Map<UUID, AirStretch> DEW_AIR = new HashMap<>();

    private static final Map<UUID, Boolean> MIDAIR_JUMP_REPORTED = new HashMap<>();

    private static final int PIN_SLOW_AMPLIFIER = 6;

    private static final int PIN_WEAKNESS_AMPLIFIER = 4;

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        DEW_AIR.remove(uuid);
        MIDAIR_JUMP_REPORTED.remove(uuid);
    }

    private CakeEvents() {
    }
}
