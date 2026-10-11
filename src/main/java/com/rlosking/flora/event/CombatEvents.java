package com.rlosking.flora.event;

import com.rlosking.flora.FloraConfig;
import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.init.ModEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.rlosking.flora.KaleidoscopeFlora;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.rlosking.flora.event.DrinkEvents;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class CombatEvents {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        if (target.hasEffect(ModEffects.ECHO)
                && event.getAmount() >= target.getHealth() + target.getAbsorptionAmount()
                && target.level() instanceof ServerLevel server) {
            event.setCanceled(true);
            performEchoRescue(server, target);
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_FREEZING) && DrinkEvents.hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_FIRE) && DrinkEvents.hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        if (target.hasEffect(ModEffects.PETAL_VEIL)
                && event.getSource().getDirectEntity() instanceof Projectile projectile) {
            long now = target.level().getGameTime();
            Long nextAllowed = PETAL_VEIL_COOLDOWN_UNTIL.get(target.getUUID());
            if (nextAllowed != null && now < nextAllowed) {
                event.setCanceled(true);
                projectile.discard();
                return;
            }
            PETAL_VEIL_COOLDOWN_UNTIL.put(target.getUUID(), now + PETAL_VEIL_COOLDOWN_TICKS);
            event.setCanceled(true);
            projectile.discard();
            if (target instanceof ServerPlayer veiled) {
                FloraAdvancements.petalVeilBlock(veiled);
            }
            DrinkEvents.shortenEffect(target, ModEffects.PETAL_VEIL, PETAL_VEIL_BLOCK_COST_TICKS);
            if (target.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.5, 0.8, 0.5, 0.1);
                server.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return;
        }

        if (target.hasEffect(ModEffects.KISS)) {
            long now = target.level().getGameTime();
            Long last = KISS_COOLDOWN.get(target.getUUID());
            if (last == null || now - last >= KISS_COOLDOWN_TICKS) {
                KISS_COOLDOWN.put(target.getUUID(), now);
                for (LivingEntity nearby : target.level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(3.0))) {
                    if (nearby != target) {
                        nearby.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    }
                }
                if (target.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.EFFECT,
                            target.getX(), target.getY() + 1.0, target.getZ(), 40, 1.0, 0.8, 1.0, 0.05);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        float damage = event.getNewDamage();
        if (damage <= 0) {
            return;
        }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity a ? a : null;

        if (attacker != null && attacker.hasEffect(ModEffects.VAMPIRIC)) {
            attacker.heal(damage * (float) (FloraConfig.vampiricHealPercent() / 100.0));
        }

        if (attacker != null && attacker.hasEffect(ModEffects.FIREBRAND)) {
            victim.igniteForSeconds(6);
        }

        if (victim.hasEffect(ModEffects.THORNS) && attacker != null && attacker != victim) {
            float roll = victim.getRandom().nextFloat();
            float share = roll < 0.05f ? 2.0f : roll < 0.30f ? 1.0f : 0.5f;
            attacker.hurt(victim.damageSources().thorns(victim), damage * share);
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }

        Long graceUntil = ECHO_GRACE_UNTIL.get(victim.getUUID());
        if (graceUntil != null) {
            if (victim.level().getGameTime() > graceUntil) {
                ECHO_GRACE_UNTIL.remove(victim.getUUID());
            } else {
                float floor = Math.min(2.0f, victim.getMaxHealth());
                if (event.getNewDamage() > victim.getHealth() - floor) {
                    event.setNewDamage(Math.max(0.0f, victim.getHealth() - floor));
                }
            }
        }
    }

    public static void performEchoRescue(ServerLevel server, LivingEntity target) {
        target.removeEffect(ModEffects.ECHO);
        ECHO_GRACE_UNTIL.put(target.getUUID(), server.getGameTime() + ECHO_GRACE_TICKS);

        if (target instanceof ServerPlayer rescued) {
            FloraAdvancements.award(rescued, FloraAdvancements.EVENT_CHEATED_DEATH);
        }

        double fromX = target.getX();
        double fromY = target.getY();
        double fromZ = target.getZ();
        server.sendParticles(ParticleTypes.PORTAL,
                fromX, fromY + 1.0, fromZ, 40, 0.5, 1.0, 0.5, 0.3);

        BlockPos spot = findStandableSpot(server, target, target.getRandom());
        if (spot != null) {
            double x = spot.getX() + 0.5;
            double y = spot.getY();
            double z = spot.getZ() + 0.5;
            target.teleportTo(x, y, z);
            server.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 40, 0.5, 1.0, 0.5, 0.3);
            server.playSound(null, x, y, z,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            server.playSound(null, fromX, fromY, fromZ,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        target.setHealth(Math.min(2.0f, target.getMaxHealth()));
        target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ECHO_GRACE_TICKS, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ECHO_GRACE_TICKS, 4));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, ECHO_GRACE_TICKS, 4));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ECHO_GRACE_TICKS, 4));
    }

    public static BlockPos findStandableSpot(ServerLevel level, LivingEntity entity, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos pos = entity.blockPosition()
                    .offset(random.nextInt(25) - 12, random.nextInt(11) - 4, random.nextInt(25) - 12);
            BlockPos ground = pos.below();
            if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
                continue;
            }
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                continue;
            }
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
                continue;
            }
            return pos;
        }
        return null;
    }

    public static final Map<UUID, Long> KISS_COOLDOWN = new HashMap<>();

    public static final int KISS_COOLDOWN_TICKS = 100;

    public static final int PETAL_VEIL_BLOCK_COST_TICKS = 200;

    public static final Map<UUID, Long> PETAL_VEIL_COOLDOWN_UNTIL = new HashMap<>();

    public static final int PETAL_VEIL_COOLDOWN_TICKS = 10;

    public static final Map<UUID, Long> ECHO_GRACE_UNTIL = new HashMap<>();

    public static final int ECHO_GRACE_TICKS = 240;

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        KISS_COOLDOWN.remove(uuid);
        ECHO_GRACE_UNTIL.remove(uuid);
        PETAL_VEIL_COOLDOWN_UNTIL.remove(uuid);
    }

    private CombatEvents() {
    }
}
