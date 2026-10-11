package com.rlosking.flora.effect;

import com.rlosking.flora.event.FloraEvents;
import com.rlosking.flora.init.ModTags;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class FlowerPathEffect extends MobEffect {

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

    public static boolean isSheetItem(ItemStack held) {
        return held.getItem() instanceof BlockItem bi
                && bi.getBlock().defaultBlockState().is(ModTags.SHEET_BLOCKS);
    }

    public static Block wildflowers() {
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
