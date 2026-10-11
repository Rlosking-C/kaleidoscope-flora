package com.rlosking.flora.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;
import com.rlosking.flora.init.ModEffects;
import com.rlosking.flora.init.BlossomMooncakes;
import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.FloraConfig;
import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.compat.PatchouliGuide;
import com.rlosking.flora.effect.FlowerPathEffect;
import com.rlosking.flora.effect.GazeEffect;
import com.rlosking.flora.effect.PetalWalkEffect;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraEvents {

    private static final long DAY_TICKS = 24000L;
    private static final long NIGHT_START = 12000L;

    private static final Map<UUID, Long> KISS_COOLDOWN = new HashMap<>();

    private static final Map<UUID, Long> ECHO_GRACE_UNTIL = new HashMap<>();

    private static final Map<UUID, double[]> LAST_XZ = new HashMap<>();

    private static final Map<UUID, double[]> MOVE_DELTA = new HashMap<>();

    private static final Map<UUID, BlockPos> BRUSH_TARGET = new HashMap<>();

    private static final Map<UUID, Integer> HARVEST_CHARGES_LEFT = new HashMap<>();

    private static final int HARVEST_CHARGES = 20;

    private static final Map<UUID, Integer> ABSOLVE_CHARGES_LEFT = new HashMap<>();

    private static final int ABSOLVE_CHARGES = 3;

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

    private static final int PIN_SLOW_AMPLIFIER = 6;

    private static final int PIN_WEAKNESS_AMPLIFIER = 4;

    private static final Map<UUID, AirStretch> DEW_AIR = new HashMap<>();

    private static final Map<UUID, Boolean> MIDAIR_JUMP_REPORTED = new HashMap<>();

    public static void noteMidAirJump(ServerPlayer player, boolean stillRising) {
        MIDAIR_JUMP_REPORTED.merge(player.getUUID(), stillRising,
                (existing, incoming) -> existing && incoming);
    }

    private static final int HARVEST_MIN_MULTIPLIER = 2;
    private static final int HARVEST_MAX_MULTIPLIER = 3;

    private static final int PETAL_VEIL_BLOCK_COST_TICKS = 200;

    private static final Map<UUID, Long> PETAL_VEIL_COOLDOWN_UNTIL = new HashMap<>();

    private static final int PETAL_VEIL_COOLDOWN_TICKS = 10;

    private static final int KISS_COOLDOWN_TICKS = 100;

    private static final int BRUSH_DIG_TICKS = 48;

    private static final int ECHO_GRACE_TICKS = 240;

    private static final Set<String> INSTANT_EFFECT_KEYS = Set.of(
            "effect.kaleidoscope_flora.purge",
            "effect.kaleidoscope_flora.fadeaway",
            "effect.kaleidoscope_flora.wish");

    private static final ResourceLocation[] ANCIENT_SEED_IDS = {
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "alfalfa_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "gelpitaya_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "himekaido_seed"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "kwat_wheat_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "sextlotus_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "warped_laurel_seeds")};

    private static Block paleMossBlock;

    private static Holder<MobEffect> warmthEffect;

    private FloraEvents() {
    }

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

        if (event.getSource().is(DamageTypeTags.IS_FREEZING) && hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_FIRE) && hasWarmth(target)) {
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
            shortenEffect(target, ModEffects.PETAL_VEIL, PETAL_VEIL_BLOCK_COST_TICKS);
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

    private static void performEchoRescue(ServerLevel server, LivingEntity target) {
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

    private static BlockPos findStandableSpot(ServerLevel level, LivingEntity entity, RandomSource random) {
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

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel server)) {
            return;
        }
        if (hasWarmth(entity)) {
            if (entity.getTicksFrozen() > 0) {
                entity.setTicksFrozen(0);
            }
            if (entity.isOnFire()) {
                entity.setRemainingFireTicks(0);
            }
        }
        if (server.getGameTime() % 4 == 0) {
            if (entity.hasEffect(ModEffects.PETAL_VEIL)) {
                orbitParticles(server, entity, ParticleTypes.CHERRY_LEAVES);
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

    private static void orbitParticles(ServerLevel server, LivingEntity entity, ParticleOptions particle) {
        long time = server.getGameTime();
        for (int i = 0; i < 2; i++) {
            double[] pos = orbitPos(entity, time, i);
            server.sendParticles(particle, pos[0], pos[1], pos[2], 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static double[] orbitPos(LivingEntity entity, long time, int i) {
        double angle = time / 10.0 + i * Math.PI;
        return new double[]{
                entity.getX() + Math.cos(angle) * 0.9,
                entity.getY() + 1.0 + Math.sin(time / 12.0 + i * 2.0) * 0.2,
                entity.getZ() + Math.sin(angle) * 0.9};
    }

    private static final Map<UUID, Long> LUCK_START = new HashMap<>();

    private static void coronationMotes(ServerLevel server, LivingEntity entity) {
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
            double[] pos = orbitPos(entity, time, i);
            server.sendParticles(gold, pos[0], pos[1], pos[2], 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void attractDrops(ServerLevel server, LivingEntity entity) {
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

    private static boolean isLullaby(ItemStack stack) {
        return FloraAdvancements.isFloraDrink(stack)
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("lullaby");
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player
                && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.sipStarted(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.isRecentlyHit()
                || !(event.getSource().getEntity() instanceof Player player)
                || !player.hasEffect(ModEffects.DIGESTION)) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.getMaxStackSize() > 1 && player.getRandom().nextInt(2) == 0) {
                stack.grow(1);
            }
        }
        int multiplier = player.getRandom().nextFloat() < 0.30f ? 2 : 1;
        List<ItemEntity> drops = List.copyOf(event.getDrops());
        for (int i = 1; i < multiplier; i++) {
            for (ItemEntity drop : drops) {
                ItemEntity copy = new ItemEntity(event.getEntity().level(),
                        drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                copy.setDefaultPickUpDelay();
                event.getDrops().add(copy);
            }
        }
        if (multiplier > 1 && player instanceof ServerPlayer urnBearer) {
            FloraAdvancements.urnDoubleDrop(urnBearer);
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }
        if (!(event.getBreaker() instanceof Player breaker) || !breaker.hasEffect(ModEffects.HARVEST)) {
            return;
        }
        UUID uuid = breaker.getUUID();
        int left = HARVEST_CHARGES_LEFT.getOrDefault(uuid, HARVEST_CHARGES);
        if (left <= 0) {
            HARVEST_CHARGES_LEFT.remove(uuid);
            breaker.removeEffect(ModEffects.HARVEST);
            return;
        }
        left--;
        if (left <= 0) {
            HARVEST_CHARGES_LEFT.remove(uuid);
            breaker.removeEffect(ModEffects.HARVEST);
        } else {
            HARVEST_CHARGES_LEFT.put(uuid, left);
        }
        int multiplier = HARVEST_MIN_MULTIPLIER
                + breaker.getRandom().nextInt(HARVEST_MAX_MULTIPLIER - HARVEST_MIN_MULTIPLIER + 1);
        List<ItemEntity> drops = List.copyOf(event.getDrops());
        for (int i = 1; i < multiplier; i++) {
            for (ItemEntity drop : drops) {
                ItemEntity copy = new ItemEntity(event.getLevel(),
                        drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                copy.setDefaultPickUpDelay();
                event.getLevel().addFreshEntity(copy);
            }
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof LivingEntity walker && walker.hasEffect(ModEffects.HARVEST)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onWalkFlowerDrops(BlockDropsEvent event) {
        BlockPos pos = event.getPos();
        boolean trail = FlowerPathEffect.isTrailBlock(event.getLevel(), pos);
        if (!trail) {
            if (!(event.getBreaker() instanceof Player breaker)
                    || !breaker.hasEffect(ModEffects.FLOWER_PATH)
                    || FlowerPathEffect.isSheetItem(breaker.getMainHandItem())) {
                return;
            }
            Block held = event.getState().getBlock();
            if (held == Blocks.AIR || held != FlowerPathEffect.wildflowers()) {
                return;
            }
        }
        FlowerPathEffect.untrackTrail(event.getLevel(), pos);
        event.setCanceled(true);
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel server) {
            FlowerPathEffect.clearTrail(server.dimension());
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
    public static void onBrushUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItem().is(Items.BRUSH)
                || !player.hasEffect(ModEffects.SNIFFER_SOUL)) {
            return;
        }
        HitResult hit = brushAim(player);
        if (hit.getType() != HitResult.Type.BLOCK
                || !(hit instanceof BlockHitResult blockHit)
                || !isSnifferGround(player.level().getBlockState(blockHit.getBlockPos()))) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BRUSH_TARGET.put(player.getUUID(), blockHit.getBlockPos());
        serverPlayer.serverLevel().playSound(null, blockHit.getBlockPos(), SoundEvents.SNIFFER_DIGGING,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        event.setDuration(BRUSH_DIG_TICKS);
    }

    private static HitResult brushAim(Player player) {
        return ProjectileUtil.getHitResultOnViewVector(
                player, e -> !e.isSpectator() && e.isPickable(), player.blockInteractionRange());
    }

    private static ServerPlayer digSessionPlayer(LivingEntityUseItemEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getItem().is(Items.BRUSH)
                || !BRUSH_TARGET.containsKey(player.getUUID())) {
            return null;
        }
        return player;
    }

    @SubscribeEvent
    public static void onBrushUseTick(LivingEntityUseItemEvent.Tick event) {
        ServerPlayer player = digSessionPlayer(event);
        if (player == null) {
            return;
        }
        UUID uuid = player.getUUID();
        HitResult hit = brushAim(player);
        if (!player.hasEffect(ModEffects.SNIFFER_SOUL)
                || !player.getMainHandItem().is(Items.BRUSH)
                || hit.getType() != HitResult.Type.BLOCK
                || !(hit instanceof BlockHitResult blockHit)
                || !isSnifferGround(player.level().getBlockState(blockHit.getBlockPos()))) {
            clearBrushSession(player, false);
            event.setCanceled(true);
            return;
        }
        BRUSH_TARGET.put(uuid, blockHit.getBlockPos());
    }

    @SubscribeEvent
    public static void onBrushUseFinish(LivingEntityUseItemEvent.Finish event) {
        ServerPlayer player = digSessionPlayer(event);
        if (player == null) {
            return;
        }
        UUID uuid = player.getUUID();
        ServerLevel server = player.serverLevel();
        BlockPos pos = BRUSH_TARGET.get(uuid);
        clearBrushSession(player, true);

        player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

        server.playSound(null, pos, SoundEvents.SNIFFER_DROP_SEED, SoundSource.BLOCKS, 1.0f, 1.0f);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.5, 0.3, 0.5, 0.1);

        SnifferDig loot = rollSnifferLoot(server, pos, player.getRandom());
        if (!loot.stack().isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(server,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, loot.stack());
            itemEntity.setDefaultPickUpDelay();
            server.addFreshEntity(itemEntity);
        }
        shortenEffect(player, ModEffects.SNIFFER_SOUL, loot.costTicks());
    }

    @SubscribeEvent
    public static void onBrushUseStop(LivingEntityUseItemEvent.Stop event) {
        ServerPlayer player = digSessionPlayer(event);
        if (player != null) {
            clearBrushSession(player, false);
        }
    }

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

        tickDewLanding(player, uuid);
    }

    private static void tickDewLanding(Player player, UUID uuid) {
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

    private static void clearBrushSession(ServerPlayer player, boolean completed) {
        UUID uuid = player.getUUID();
        BlockPos pos = BRUSH_TARGET.remove(uuid);
        if (pos == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(
                SoundEvents.SNIFFER_DIGGING.getLocation(), SoundSource.BLOCKS);
        for (ServerPlayer listener : level.players()) {
            listener.connection.send(stop);
        }
        if (!completed) {
            level.playSound(null, pos, SoundEvents.SNIFFER_DIGGING_STOP,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
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

    @SubscribeEvent
    public static void onPlayerTickAdvancements(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        FloraAdvancements.tick(player);
        com.rlosking.flora.compat.PatchouliGuide.tickFlowerUnlock(player);
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

    private static boolean isSnifferGround(BlockState state) {
        if (state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL)) {
            return false;
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
            return true;
        }
        if (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)) {
            return true;
        }
        if (paleMossBlock == null) {
            paleMossBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("pale_moss_block"));
        }
        return state.is(paleMossBlock);
    }

    private record SnifferDig(ItemStack stack, int costTicks) {
    }

    private enum SherdPool {
        DESERT(Items.ARCHER_POTTERY_SHERD, Items.MINER_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD,
                Items.SKULL_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD),
        WARM_OCEAN(Items.ANGLER_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.SNORT_POTTERY_SHERD),
        COLD(Items.BLADE_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.MOURNER_POTTERY_SHERD,
                Items.PLENTY_POTTERY_SHERD),
        TEMPERATE(Items.BURN_POTTERY_SHERD, Items.DANGER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD,
                Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD,
                Items.SHEAF_POTTERY_SHERD);

        final Item[] sherds;

        SherdPool(Item... sherds) {
            this.sherds = sherds;
        }
    }

    private static SherdPool sherdPoolAt(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return SherdPool.TEMPERATE;
        }
        Holder<Biome> biome = level.getBiome(pos);
        boolean cold = biome.value().getBaseTemperature() < 0.2F;
        if (biome.is(BiomeTags.IS_BADLANDS)
                || !biome.value().hasPrecipitation()) {
            return SherdPool.DESERT;
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) {
            return cold ? SherdPool.COLD : SherdPool.WARM_OCEAN;
        }
        return cold ? SherdPool.COLD : SherdPool.TEMPERATE;
    }

    private static SnifferDig rollSnifferLoot(ServerLevel level, BlockPos pos, RandomSource random) {
        int roll = random.nextInt(120);
        if (roll < 30) {
            return new SnifferDig(new ItemStack(Items.TORCHFLOWER_SEEDS), 20 * 20);
        }
        if (roll < 42) {
            return new SnifferDig(new ItemStack(Items.PITCHER_POD), 20 * 20);
        }
        if (roll < 58) {
            return new SnifferDig(new ItemStack(Items.COAL), 8 * 20);
        }
        if (roll < 72) {
            return new SnifferDig(new ItemStack(Items.IRON_NUGGET, 2), 8 * 20);
        }
        if (roll < 80) {
            return new SnifferDig(new ItemStack(Items.GOLD_NUGGET, 2), 15 * 20);
        }
        if (roll < 88) {
            return new SnifferDig(new ItemStack(Items.BONE), 10 * 20);
        }
        if (roll < 100) {
            return new SnifferDig(new ItemStack(Items.BONE_MEAL, 2), 5 * 20);
        }
        if (roll < 110) {
            Item[] sherds = sherdPoolAt(level, pos).sherds;
            return new SnifferDig(new ItemStack(sherds[random.nextInt(sherds.length)]), 15 * 20);
        }
        if (roll < 118) {
            ItemStack seeds = rollAncientSeed(random);
            return seeds.isEmpty()
                    ? new SnifferDig(new ItemStack(Items.COAL), 8 * 20)
                    : new SnifferDig(seeds, 25 * 20);
        }
        return new SnifferDig(new ItemStack(Items.SNIFFER_EGG), 60 * 20);
    }

    private static ItemStack rollAncientSeed(RandomSource random) {
        Item item = BuiltInRegistries.ITEM.get(ANCIENT_SEED_IDS[random.nextInt(ANCIENT_SEED_IDS.length)]);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

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

    private static void shortenEffect(LivingEntity target, Holder<MobEffect> effect, int costTicks) {
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

    private static boolean hasWarmth(LivingEntity entity) {
        if (warmthEffect == null) {
            warmthEffect = BuiltInRegistries.MOB_EFFECT.getHolder(
                    ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "warmth")).orElse(null);
        }
        return warmthEffect != null && entity.hasEffect(warmthEffect);
    }

    public static double[] lastMove(Player player) {
        double[] delta = MOVE_DELTA.get(player.getUUID());
        return delta == null ? new double[]{0.0, 0.0} : delta;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        com.rlosking.flora.compat.PatchouliGuide.giveOnce(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        KISS_COOLDOWN.remove(uuid);
        ECHO_GRACE_UNTIL.remove(uuid);
        LAST_XZ.remove(uuid);
        MOVE_DELTA.remove(uuid);
        LUCK_START.remove(uuid);
        HARVEST_CHARGES_LEFT.remove(uuid);
        ABSOLVE_CHARGES_LEFT.remove(uuid);
        PETAL_VEIL_COOLDOWN_UNTIL.remove(uuid);
        DEW_AIR.remove(uuid);
        MIDAIR_JUMP_REPORTED.remove(uuid);
        FlowerPathEffect.clearPlayer(uuid);
        GazeEffect.clearPlayer(uuid);
        PetalWalkEffect.clearPlayer(uuid);
        FloraAdvancements.clearPlayer(uuid);
        clearBrushSession((ServerPlayer) event.getEntity(), false);
    }
}
