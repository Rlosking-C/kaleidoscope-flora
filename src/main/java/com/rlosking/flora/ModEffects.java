package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KaleidoscopeFlora.MOD_ID);

    static int scaledTicks(int seconds) {
        double multiplier = FloraConfig.SPEC.isLoaded()
                ? FloraConfig.effectDurationMultiplier()
                : 1.0;
        return Math.max(1, (int) (seconds * 20 * multiplier));
    }

    public static final DeferredHolder<MobEffect, PurgeEffect> PURGE =
            EFFECTS.register("purge", PurgeEffect::new);

    public static final DeferredHolder<MobEffect, FadeawayEffect> FADEAWAY =
            EFFECTS.register("fadeaway", FadeawayEffect::new);

    public static final DeferredHolder<MobEffect, DrowsyAuraEffect> DROWSY =
            EFFECTS.register("drowsy", DrowsyAuraEffect::new);

    public static final DeferredHolder<MobEffect, PetalWalkEffect> PETALWALK =
            EFFECTS.register("petalwalk", PetalWalkEffect::new);

    public static final DeferredHolder<MobEffect, DivinationEffect> DIVINATION =
            EFFECTS.register("divination", DivinationEffect::new);

    public static final DeferredHolder<MobEffect, WitherAuraEffect> WITHER_AURA =
            EFFECTS.register("wither_aura", WitherAuraEffect::new);

    public static final DeferredHolder<MobEffect, SunwardEffect> SUNWARD =
            EFFECTS.register("sunward", SunwardEffect::new);

    public static final DeferredHolder<MobEffect, SproutEffect> SPROUT =
            EFFECTS.register("sprout", SproutEffect::new);

    public static final DeferredHolder<MobEffect, GazeEffect> GAZE =
            EFFECTS.register("gaze", GazeEffect::new);

    public static final DeferredHolder<MobEffect, WishEffect> WISH =
            EFFECTS.register("wish", WishEffect::new);

    public static final DeferredHolder<MobEffect, FlowerPathEffect> FLOWER_PATH =
            EFFECTS.register("flower_path", FlowerPathEffect::new);

    public static final DeferredHolder<MobEffect, FloatingEffect> FLOATING =
            EFFECTS.register("floating", FloatingEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> TOASTED_CAKE =
            EFFECTS.register("toasted_cake", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xD9A441));

    public static final DeferredHolder<MobEffect, MobEffect> DEW_CAKE =
            EFFECTS.register("dew_cake", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x8FD8E8));

    public static final DeferredHolder<MobEffect, MobEffect> PINNED =
            EFFECTS.register("pinned", PinnedEffect::new);

    public static final int PIN_TICKS = 20;

    private static final double PIN_MIN_RADIUS = 0.45;

    private static final double PIN_RADIUS_PER_WIDTH = 0.75;

    private static final double PIN_PARTICLE_SPACING = 0.4;

    private static final int PIN_MIN_PARTICLES = 6;

    private static final int PIN_MAX_PARTICLES = 48;

    private static final double PIN_LIFT_PER_HEIGHT = 0.35;
    private static final double PIN_MIN_LIFT = 0.15;
    private static final double PIN_MAX_LIFT = 0.6;

    public static double pinRingRadius(LivingEntity entity) {
        return Math.max(PIN_MIN_RADIUS, entity.getBbWidth() * PIN_RADIUS_PER_WIDTH);
    }

    public static int pinRingCount(double radius) {
        int count = (int) Math.round(radius * 2.0 * Math.PI / PIN_PARTICLE_SPACING);
        return Math.max(PIN_MIN_PARTICLES, Math.min(PIN_MAX_PARTICLES, count));
    }

    public static double pinRingLift(LivingEntity entity) {
        return Math.max(PIN_MIN_LIFT, Math.min(PIN_MAX_LIFT, entity.getBbHeight() * PIN_LIFT_PER_HEIGHT));
    }

    public static class PinnedEffect extends MobEffect {

        public PinnedEffect() {
            super(MobEffectCategory.HARMFUL, 0xC8E4F0);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel server)) {
                return true;
            }
            MobEffectInstance self = entity.getEffect(PINNED);
            double radius = pinRingRadius(entity);
            int count = pinRingCount(radius);
            double lift = pinRingLift(entity);
            for (int i = 0; i < count; i++) {
                double angle = (i / (double) count) * Math.PI * 2.0;
                server.sendParticles(ParticleTypes.END_ROD,
                        entity.getX() + Math.cos(angle) * radius,
                        entity.getY() + lift,
                        entity.getZ() + Math.sin(angle) * radius,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
            return true;
        }
    }

    public static final DeferredHolder<MobEffect, MobEffect> TASTEBLOOM =
            EFFECTS.register("tastebloom", TastebloomEffect::new);

    public static final DeferredHolder<MobEffect, MobEffect> FIREBRAND =
            EFFECTS.register("firebrand", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE86A17));

    public static final DeferredHolder<MobEffect, MobEffect> VAMPIRIC =
            EFFECTS.register("vampiric", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xB02E26));

    public static final DeferredHolder<MobEffect, MobEffect> HARVEST =
            EFFECTS.register("harvest", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE8A13A));

    public static final DeferredHolder<MobEffect, MobEffect> ABSOLVE =
            EFFECTS.register("absolve", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF2F2F2));

    public static final DeferredHolder<MobEffect, MobEffect> FEATHERFALL =
            EFFECTS.register("featherfall", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x9ED9F0));

    public static final DeferredHolder<MobEffect, MobEffect> KISS =
            EFFECTS.register("kiss", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xDCEDC2));

    public static final DeferredHolder<MobEffect, MobEffect> THORNS =
            EFFECTS.register("thorns", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC62828));

    public static final DeferredHolder<MobEffect, MobEffect> DIGESTION =
            EFFECTS.register("digestion", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x7BAF6A));

    public static final DeferredHolder<MobEffect, MobEffect> PETAL_VEIL =
            EFFECTS.register("petal_veil", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF5B5D9));

    public static final DeferredHolder<MobEffect, MobEffect> SNIFFER_SOUL =
            EFFECTS.register("sniffer_soul", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC8814F));

    public static final DeferredHolder<MobEffect, MobEffect> ECHO =
            EFFECTS.register("echo", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xA59BB8));

    public static class MarkerEffect extends MobEffect {
        public MarkerEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    public static boolean isAirborne(LivingEntity entity) {
        return !entity.onGround() && entity.hasEffect(FLOATING);
    }

    public static class PurgeEffect extends MobEffect {

        static final int PURGE_MAX_EFFECTS = 3;

        public PurgeEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xF5F0C8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            int cleared = 0;
            for (MobEffectInstance instance : List.copyOf(entity.getActiveEffects())) {
                if (cleared >= PURGE_MAX_EFFECTS) {
                    break;
                }
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    entity.removeEffect(instance.getEffect());
                    cleared++;
                }
            }
            return true;
        }
    }

    public static class FadeawayEffect extends MobEffect {
        public FadeawayEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xD8D8C8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            double range = 24.0 * FloraConfig.auraRangeMultiplier();
            for (Monster monster : entity.level().getEntitiesOfClass(Monster.class,
                    entity.getBoundingBox().inflate(range))) {
                if (monster.getTarget() == entity) {
                    monster.setTarget(null);
                }
            }
            for (Phantom phantom : entity.level().getEntitiesOfClass(Phantom.class,
                    entity.getBoundingBox().inflate(range))) {
                if (phantom.getTarget() == entity) {
                    phantom.setTarget(null);
                }
            }
            return true;
        }
    }

    public static class TastebloomEffect extends MobEffect {

        static final int TASTEBLOOM_HASTE_FOOD_LEVEL = 20;

        public TastebloomEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xF7E3A1);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide || !(entity instanceof Player player)) {
                return true;
            }
            if (player.getFoodData().getFoodLevel() >= TASTEBLOOM_HASTE_FOOD_LEVEL) {
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false));
            }
            return true;
        }
    }

    public static class DrowsyAuraEffect extends MobEffect {

        private static final int CUP_SECONDS = 180;

        private static final Map<UUID, long[]> LAST_CUP = new HashMap<>();

        private static boolean stacking;

        public DrowsyAuraEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9C8FB8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            Level level = entity.level();
            double range = 7.0 * FloraConfig.auraRangeMultiplier();
            for (Monster monster : level.getEntitiesOfClass(Monster.class,
                    entity.getBoundingBox().inflate(range))) {
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            }
            for (Phantom phantom : level.getEntitiesOfClass(Phantom.class,
                    entity.getBoundingBox().inflate(range))) {
                if (phantom.getTarget() == entity) {
                    phantom.setTarget(null);
                }
            }
            return true;
        }

        @Override
        public void onEffectStarted(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide || stacking) {
                return;
            }
            if (!(entity instanceof Player player)) {
                return;
            }
            MobEffectInstance active = player.getEffect(ModEffects.DROWSY);
            if (active == null) {
                return;
            }
            long now = entity.level().getGameTime();
            long[] last = LAST_CUP.get(player.getUUID());
            if (last == null) {
                LAST_CUP.put(player.getUUID(), new long[] {now, active.getDuration()});
                return;
            }
            long remaining = Math.max(0L, last[1] - (now - last[0]));
            if (remaining == 0) {
                LAST_CUP.put(player.getUUID(), new long[] {now, active.getDuration()});
                return;
            }
            int desired = (int) Math.min(remaining + (long) CUP_SECONDS * 20, Integer.MAX_VALUE);
            stacking = true;
            try {
                player.addEffect(new MobEffectInstance(ModEffects.DROWSY, desired,
                        active.getAmplifier()));
            } finally {
                stacking = false;
            }
            LAST_CUP.put(player.getUUID(), new long[] {now, desired});
        }
    }

    public static class PetalWalkEffect extends MobEffect {
        private static final Map<UUID, Long> SPLASH_CLOCK = new HashMap<>();

        public static void clearPlayer(UUID uuid) {
            SPLASH_CLOCK.remove(uuid);
        }

        public static final double PLUNGE_SPEED = 0.55;

        public PetalWalkEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xEBA2C8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            Level level = entity.level();
            if (level.isClientSide()) {
                return true;
            }
            BlockPos under = BlockPos.containing(entity.getX(), entity.getY() - 0.15, entity.getZ());
            if (!entity.onGround() || !level.getFluidState(under).is(FluidTags.WATER)) {
                return true;
            }
            ServerLevel server = (ServerLevel) level;
            double strideDist = entity instanceof Player walker
                    ? Math.hypot(FloraEvents.lastMove(walker)[0], FloraEvents.lastMove(walker)[1])
                    : entity.getDeltaMovement().horizontalDistance();
            boolean striding = strideDist > 0.01;
            if (striding) {
                if (entity.getRandom().nextInt(2) == 0) {
                    server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                            entity.getX(), entity.getY() + 0.1, entity.getZ(), 2, 0.3, 0.02, 0.3, 0.01);
                }
                server.sendParticles(ParticleTypes.SPLASH,
                        entity.getX(), entity.getY() + 0.1, entity.getZ(), 8, 0.4, 0.1, 0.4, 0.05);
                server.sendParticles(ParticleTypes.FALLING_WATER,
                        entity.getX(), entity.getY() + 0.4, entity.getZ(), 2, 0.3, 0.05, 0.3, 0.0);
                Long lastSplash = SPLASH_CLOCK.get(entity.getUUID());
                if (lastSplash == null || System.currentTimeMillis() - lastSplash >= 250L) {
                    SPLASH_CLOCK.put(entity.getUUID(), System.currentTimeMillis());
                    level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_SPLASH,
                            SoundSource.PLAYERS, 0.12f, 1.0f + entity.getRandom().nextFloat() * 0.3f);
                }
            } else if (entity.getRandom().nextInt(4) == 0) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        entity.getX(), entity.getY() + 0.1, entity.getZ(), 2, 0.3, 0.02, 0.3, 0.01);
            }
            return true;
        }
    }

    public static class DivinationEffect extends MobEffect {
        public DivinationEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xEFE9C0);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 160 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            switch (entity.getRandom().nextInt(4)) {
                case 0 -> entity.heal(2.0f);
                case 1 -> {
                    if (entity instanceof Player player) {
                        player.getFoodData().eat(1, 0.4f);
                    }
                }
                case 2 -> entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
                default -> {
                }
            }
            return true;
        }
    }

    public static class WitherAuraEffect extends MobEffect {
        public WitherAuraEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x3A3A3A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            ServerLevel server = (ServerLevel) entity.level();
            double range = 7.0 * FloraConfig.auraRangeMultiplier();
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(range))) {
                if (other == entity) {
                    continue;
                }
                other.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
                server.sendParticles(ParticleTypes.SMOKE,
                        other.getX(), other.getY() + other.getBbHeight() * 0.5, other.getZ(),
                        3, 0.15, 0.25, 0.15, 0.01);
            }
            return true;
        }
    }

    public static class SunwardEffect extends MobEffect {
        public SunwardEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xFFD835);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            Level level = entity.level();
            if (level.isDay() && level.canSeeSky(entity.blockPosition())) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
                if (entity instanceof ServerPlayer player) {
                    FloraAdvancements.sunwardDayForm(player);
                }
            } else if (!level.isDay()) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
            }
            return true;
        }
    }

    public static class SproutEffect extends MobEffect {
        public SproutEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x7BBF4A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            ServerLevel server = (ServerLevel) entity.level();
            BlockPos center = entity.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                if (entity.getRandom().nextFloat() >= 0.25f) {
                    continue;
                }
                if (!ripenOne(server, pos, entity)) {
                    continue;
                }
                if (entity instanceof ServerPlayer player) {
                    FloraAdvancements.sproutCropRipened(player);
                }
            }
            return true;
        }

        private static boolean ripenOne(ServerLevel level, BlockPos pos, LivingEntity entity) {
            BlockState state = level.getBlockState(pos);
            boolean cropLike = state.getBlock() instanceof CropBlock || ageProperty(state) != null;
            if (!cropLike) {
                return false;
            }
            if (state.getBlock() instanceof CropBlock && state.hasProperty(CropBlock.AGE)) {
                int age = state.getValue(CropBlock.AGE);
                if (age >= MAX_CROP_AGE) {
                    return false;
                }
                level.setBlock(pos, state.setValue(CropBlock.AGE, age + 1), 2);
                return true;
            }
            if (!(state.getBlock() instanceof BonemealableBlock growable)) {
                return false;
            }
            if (!growable.isValidBonemealTarget(level, pos, state)
                    || !growable.isBonemealSuccess(level, entity.getRandom(), pos, state)) {
                return false;
            }
            growable.performBonemeal(level, entity.getRandom(), pos, state);
            return true;
        }

        private static final int MAX_CROP_AGE = CropBlock.AGE.getPossibleValues().stream()
                .mapToInt(Integer::intValue).max().orElse(7);

        private static IntegerProperty ageProperty(BlockState state) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof IntegerProperty age && "age".equals(property.getName())) {
                    return age;
                }
            }
            return null;
        }
    }

    public static class GazeEffect extends MobEffect {

        private static final int FREEZE_TICKS = 80;

        private static final long STALE_GAP_TICKS = 5;

        private static final Map<UUID, Map<UUID, Integer>> STARE = new HashMap<>();

        private static final Map<UUID, Long> STARE_CLOCK = new HashMap<>();

        public static void clearPlayer(UUID uuid) {
            STARE.remove(uuid);
            STARE_CLOCK.remove(uuid);
        }

        public GazeEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9FB6D9);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            ServerLevel server = (ServerLevel) entity.level();
            UUID drinker = entity.getUUID();
            long now = server.getGameTime();

            Map<UUID, Integer> stares = STARE.computeIfAbsent(drinker, k -> new HashMap<>());
            Long lastTick = STARE_CLOCK.get(drinker);
            if (lastTick == null || now - lastTick > STALE_GAP_TICKS) {
                stares.clear();
            }
            STARE_CLOCK.put(drinker, now);

            Set<UUID> stared = new HashSet<>();
            Vec3 eye = entity.getEyePosition();
            Vec3 look = entity.getViewVector(1.0F);
            double gazeRange = 25.0 * FloraConfig.auraRangeMultiplier();
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(gazeRange))) {
                if (other == entity) {
                    continue;
                }
                if (now % 10 == 0) {
                    other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false));
                }
                Vec3 toOther = other.getEyePosition().subtract(eye);
                double distance = toOther.length();
                boolean seen = distance < 0.5
                        || (look.dot(toOther.normalize()) >= 0.966 && entity.hasLineOfSight(other));
                if (!seen) {
                    continue;
                }
                stared.add(other.getUUID());
                if (other instanceof EnderMan && entity instanceof ServerPlayer starer) {
                    FloraAdvancements.award(starer, FloraAdvancements.EVENT_GAZE_ABYSS);
                }
                int ticks = stares.getOrDefault(other.getUUID(), 0) + 1;
                stares.put(other.getUUID(), ticks);
                if (ticks < FREEZE_TICKS) {
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, true, true));
                } else {
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
                }
            }
            stares.keySet().retainAll(stared);
            return true;
        }
    }

    public static class WishEffect extends MobEffect {
        public WishEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xFFD700);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            int blessing = scaledTicks(120);
            MobEffectInstance[] pool = {
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, blessing, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, blessing, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, blessing, 1),
                    new MobEffectInstance(MobEffects.DIG_SPEED, blessing, 1),
                    new MobEffectInstance(MobEffects.LUCK, blessing, 0)};
            entity.addEffect(pool[entity.getRandom().nextInt(pool.length)]);
            if (entity.level() instanceof ServerLevel server) {
                server.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.0F), 0.9F),
                        entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
            }
            return true;
        }
    }

    public static class FlowerPathEffect extends MobEffect {

        private static Block wildflowersBlock;

        private static final Map<UUID, BlockPos> STAND_POS = new HashMap<>();

        private static final Map<UUID, Integer> STAND_TICKS = new HashMap<>();

        private static final Map<ResourceKey<Level>, Set<BlockPos>> TRAIL = new HashMap<>();

        private static final int TRAIL_SWEEP_THRESHOLD = 4096;

        private static final int TICKS_PER_FLOWER = 20;

        public static void clearPlayer(UUID uuid) {
            STAND_POS.remove(uuid);
            STAND_TICKS.remove(uuid);
        }

        public FlowerPathEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9CDF6A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide || !(entity instanceof Player player)) {
                return true;
            }
            if (!player.onGround()) {
                STAND_POS.remove(player.getUUID());
                return true;
            }
            double[] step = FloraEvents.lastMove(player);
            double dist = Math.hypot(step[0], step[1]);
            Level level = player.level();
            ItemStack held = player.getMainHandItem();
            boolean layingFromHand = isSheetItem(held);

            UUID uuid = player.getUUID();
            BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
            int dwell = feet.equals(STAND_POS.get(uuid)) ? STAND_TICKS.getOrDefault(uuid, 0) + 1 : 0;
            STAND_POS.put(uuid, feet);
            STAND_TICKS.put(uuid, dwell);
            if (!layingFromHand) {
                growFlowers(level, feet, 1 + Math.min(3, dwell / TICKS_PER_FLOWER));
            }

            if (dist < 0.02) {
                if (!layingFromHand) {
                    layWildflowers(level, feet);
                }
                return true;
            }

            BlockItem blockItem = layingFromHand ? (BlockItem) held.getItem() : null;
            int steps = Math.max(1, Mth.ceil(dist / 0.4));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                BlockPos pos = BlockPos.containing(
                        player.getX() - step[0] * (1.0 - t),
                        player.getY(),
                        player.getZ() - step[1] * (1.0 - t));
                if (blockItem != null && !held.isEmpty()) {
                    laySheet(level, player, held, blockItem, pos);
                } else {
                    layWildflowers(level, pos);
                }
            }
            return true;
        }

        static boolean isSheetItem(ItemStack held) {
            return held.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(ModTags.SHEET_BLOCKS);
        }

        static Block wildflowers() {
            if (wildflowersBlock == null) {
                wildflowersBlock = BuiltInRegistries.BLOCK.get(
                        ResourceLocation.withDefaultNamespace("wildflowers"));
            }
            return wildflowersBlock;
        }

        private static void growFlowers(Level level, BlockPos pos, int target) {
            BlockState state = level.getBlockState(pos);
            for (Property<?> property : state.getProperties()) {
                if ("flower_amount".equals(property.getName()) && property instanceof IntegerProperty amount) {
                    int current = state.getValue(amount);
                    int wanted = Math.min(target, amount.getPossibleValues().stream()
                            .mapToInt(Integer::intValue).max().orElse(current));
                    if (current >= wanted) {
                        return;
                    }
                    level.setBlock(pos, state.setValue(amount, wanted), 3);
                    if (level instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.15, 0.3, 0.02);
                    }
                    return;
                }
            }
        }

        private static void layWildflowers(Level level, BlockPos pos) {
            Block wildflowersBlock = wildflowers();
            if (wildflowersBlock == Blocks.AIR
                    || !level.getBlockState(pos).canBeReplaced()
                    || !level.getFluidState(pos).isEmpty()) {
                return;
            }
            BlockState state = wildflowersBlock.defaultBlockState();
            if (!state.canSurvive(level, pos)) {
                return;
            }
            level.setBlock(pos, state, 3);
            trackTrail(level, pos);
            SoundType sound = state.getSoundType(level, pos, null);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, 0.7f, 1.0f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.15, 0.3, 0.02);
            }
        }

        private static void trackTrail(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            Set<BlockPos> trail = TRAIL.computeIfAbsent(server.dimension(), k -> new HashSet<>());
            trail.add(pos.immutable());
            if (trail.size() > TRAIL_SWEEP_THRESHOLD) {
                Block flowers = wildflowers();
                trail.removeIf(p -> !server.getBlockState(p).is(flowers));
            }
        }

        public static boolean isTrailBlock(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server)) {
                return false;
            }
            Set<BlockPos> trail = TRAIL.get(server.dimension());
            return trail != null && trail.contains(pos);
        }

        public static void untrackTrail(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            Set<BlockPos> trail = TRAIL.get(server.dimension());
            if (trail != null) {
                trail.remove(pos);
            }
        }

        public static void clearTrail(ResourceKey<Level> dimension) {
            TRAIL.remove(dimension);
        }

        private static void laySheet(Level level, Player player, ItemStack held, BlockItem blockItem, BlockPos pos) {
            if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
                return;
            }
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
            BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held, hit);
            BlockState state = blockItem.getBlock().getStateForPlacement(context);
            if (state == null || !state.canSurvive(level, pos)) {
                return;
            }
            level.setBlock(pos, state, 3);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            SoundType sound = state.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, 0.7f, 1.0f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.15, 0.3, 0.02);
            }
        }
    }

    public static class FloatingEffect extends MobEffect {
        public FloatingEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xBBD4F5);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            entity.resetFallDistance();
            return true;
        }
    }
}
