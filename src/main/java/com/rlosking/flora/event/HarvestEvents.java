package com.rlosking.flora.event;

import com.rlosking.flora.effect.FlowerPathEffect;
import com.rlosking.flora.init.FloraAdvancements;
import com.rlosking.flora.init.ModEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import com.rlosking.flora.KaleidoscopeFlora;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.rlosking.flora.event.DrinkEvents;

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class HarvestEvents {

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
        DrinkEvents.shortenEffect(player, ModEffects.SNIFFER_SOUL, loot.costTicks());
    }

    @SubscribeEvent
    public static void onBrushUseStop(LivingEntityUseItemEvent.Stop event) {
        ServerPlayer player = digSessionPlayer(event);
        if (player != null) {
            clearBrushSession(player, false);
        }
    }

    public static HitResult brushAim(Player player) {
        return ProjectileUtil.getHitResultOnViewVector(
                player, e -> !e.isSpectator() && e.isPickable(), player.blockInteractionRange());
    }

    public static ServerPlayer digSessionPlayer(LivingEntityUseItemEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getItem().is(Items.BRUSH)
                || !BRUSH_TARGET.containsKey(player.getUUID())) {
            return null;
        }
        return player;
    }

    public static void clearBrushSession(ServerPlayer player, boolean completed) {
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

    public static boolean isSnifferGround(BlockState state) {
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

    public static SherdPool sherdPoolAt(ServerLevel level, BlockPos pos) {
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

    public static SnifferDig rollSnifferLoot(ServerLevel level, BlockPos pos, RandomSource random) {
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

    public static ItemStack rollAncientSeed(RandomSource random) {
        Item item = BuiltInRegistries.ITEM.get(ANCIENT_SEED_IDS[random.nextInt(ANCIENT_SEED_IDS.length)]);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static final Map<UUID, BlockPos> BRUSH_TARGET = new HashMap<>();

    public static final int BRUSH_DIG_TICKS = 48;

    public static final Map<UUID, Integer> HARVEST_CHARGES_LEFT = new HashMap<>();

    public static final int HARVEST_CHARGES = 20;

    public static final int HARVEST_MIN_MULTIPLIER = 2;

    public static final int HARVEST_MAX_MULTIPLIER = 3;

    public static final ResourceLocation[] ANCIENT_SEED_IDS = {
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "alfalfa_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "gelpitaya_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "himekaido_seed"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "kwat_wheat_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "sextlotus_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "warped_laurel_seeds")};

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        HARVEST_CHARGES_LEFT.remove(uuid);
        clearBrushSession((ServerPlayer) event.getEntity(), false);
    }

    record SnifferDig(ItemStack stack, int costTicks) {
    }

    enum SherdPool {
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

    static Block paleMossBlock;

    private HarvestEvents() {
    }
}
