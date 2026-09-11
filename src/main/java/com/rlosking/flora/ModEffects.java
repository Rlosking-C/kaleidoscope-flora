package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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

/**
 * All 23 custom MobEffects of Kaleidoscope Flora.
 *
 * <p>Two flavours of effects live here:</p>
 * <ul>
 *   <li><b>Ticking / instant effects</b> (11 classes below) - they carry their
 *       logic inside {@code applyEffectTick} (auras, water walking, crops,
 *       day/night switching, random boons...).</li>
 *   <li><b>Marker effects</b> (12 plain registrations) - they have no code at
 *       all. They only exist so {@code hasEffect(...)} can be checked inside
 *       {@link FloraEvents}, which implements their behaviour through damage
 *       / interact / loot events (thorns, vampirism, looting, petal veil...).</li>
 * </ul>
 *
 * <p><b>1.21.1 API note:</b> {@code applyEffectTick} returns a boolean and the
 * vanilla default of {@code shouldApplyEffectTickThisTick} is {@code false} -
 * any effect whose tick should ever run MUST override it. Effects whose
 * {@code applyEffectTick} returns {@code false} are removed from the entity
 * immediately, and {@code applyEffectTick} also runs on the CLIENT (where the
 * level is not a ServerLevel), so every implementation here follows two rules:
 * guard with {@code isClientSide} and only ever return {@code true}.</p>
 *
 * <p>Effect lang keys follow the vanilla convention and resolve to
 * {@code effect.kaleidoscope_flora.<id>} automatically.</p>
 */
public final class ModEffects {

    /** Mob effect deferred register; hooked to the mod bus by the main class. */
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KaleidoscopeFlora.MOD_ID);

    // ------------------------------------------------------------------
    // Ticking / instant effects (logic lives in this file)
    // ------------------------------------------------------------------

    /** Dandelion "When the Wind Rises": instantly clears every harmful effect. */
    public static final DeferredHolder<MobEffect, PurgeEffect> PURGE =
            EFFECTS.register("purge", PurgeEffect::new);

    /** Azure bluet "The Unnoticed": instantly drops all mobs' aggro on you. */
    public static final DeferredHolder<MobEffect, FadeawayEffect> FADEAWAY =
            EFFECTS.register("fadeaway", FadeawayEffect::new);

    /** Poppy "Lullaby": drowsiness aura that slows hostiles and pacifies phantoms. */
    public static final DeferredHolder<MobEffect, DrowsyAuraEffect> DROWSY =
            EFFECTS.register("drowsy", DrowsyAuraEffect::new);

    /** Pink tulip "Rosy Stride": walk on water while it lasts. */
    public static final DeferredHolder<MobEffect, PetalWalkEffect> PETALWALK =
            EFFECTS.register("petalwalk", PetalWalkEffect::new);

    /** Oxeye daisy "Loves Me Not": rolls a random boon every 8 seconds. */
    public static final DeferredHolder<MobEffect, DivinationEffect> DIVINATION =
            EFFECTS.register("divination", DivinationEffect::new);

    /** Wither rose "Les Fleurs du Mal": wither aura around the drinker. */
    public static final DeferredHolder<MobEffect, WitherAuraEffect> WITHER_AURA =
            EFFECTS.register("wither_aura", WitherAuraEffect::new);

    /** Sunflower "The Sunward": regeneration by day, glowing by night. */
    public static final DeferredHolder<MobEffect, SunwardEffect> SUNWARD =
            EFFECTS.register("sunward", SunwardEffect::new);

    /** Spore blossom "Vernal Awakening": randomly grows nearby crops. */
    public static final DeferredHolder<MobEffect, SproutEffect> SPROUT =
            EFFECTS.register("sprout", SproutEffect::new);

    /** Eyeblossom "The Gaze": everything in 25 blocks glows; a straight,
     * unobstructed stare pins the target (4 s freeze, then slow). */
    public static final DeferredHolder<MobEffect, GazeEffect> GAZE =
            EFFECTS.register("gaze", GazeEffect::new);

    /** Golden dandelion "As You Wish": grants one random tier-II buff. */
    public static final DeferredHolder<MobEffect, WishEffect> WISH =
            EFFECTS.register("wish", WishEffect::new);

    /** Wildflowers "Springtime Stroll": held sheet blocks are laid underfoot while walking. */
    public static final DeferredHolder<MobEffect, FlowerPathEffect> FLOWER_PATH =
            EFFECTS.register("flower_path", FlowerPathEffect::new);

    // ------------------------------------------------------------------
    // Marker effects (no code; behaviour lives in FloraEvents)
    // ------------------------------------------------------------------

    /** Blue orchid "First Bloom": eaten food restores 50% more nutrition. */
    public static final DeferredHolder<MobEffect, MobEffect> TASTEBLOOM =
            EFFECTS.register("tastebloom", TastebloomEffect::new);

    /** Allium "Fire Waltz": melee hits ignite the target. */
    public static final DeferredHolder<MobEffect, MobEffect> FIREBRAND =
            EFFECTS.register("firebrand", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE86A17));

    /** Red tulip "Crimson Heartbeat": 15% of melee damage returns as health. */
    public static final DeferredHolder<MobEffect, MobEffect> VAMPIRIC =
            EFFECTS.register("vampiric", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xB02E26));

    /** Orange tulip "Autumn Serenade": breaking mature crops multiplies the drops (x2-x4). */
    public static final DeferredHolder<MobEffect, MobEffect> HARVEST =
            EFFECTS.register("harvest", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE8A13A));

    /** White tulip "Absolution": immune to newly applied harmful effects. */
    public static final DeferredHolder<MobEffect, MobEffect> ABSOLVE =
            EFFECTS.register("absolve", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF2F2F2));

    /** Cornflower "The Prussian Leap": fall damage immunity. */
    public static final DeferredHolder<MobEffect, MobEffect> FEATHERFALL =
            EFFECTS.register("featherfall", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x9ED9F0));

    /** Lily of the valley "May Kiss": hurt = poison burst around you. */
    public static final DeferredHolder<MobEffect, MobEffect> KISS =
            EFFECTS.register("kiss", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xDCEDC2));

    /** Rose bush "Tender Thorns": attackers take reflected damage + weakness. */
    public static final DeferredHolder<MobEffect, MobEffect> THORNS =
            EFFECTS.register("thorns", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC62828));

    /** Pitcher plant "The Voracious Urn": kills count as +1 looting level. */
    public static final DeferredHolder<MobEffect, MobEffect> DIGESTION =
            EFFECTS.register("digestion", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x7BAF6A));

    /** Pink petals "Hanami Tale": petals block projectiles, spending duration. */
    public static final DeferredHolder<MobEffect, MobEffect> PETAL_VEIL =
            EFFECTS.register("petal_veil", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF5B5D9));

    /** Torchflower "Breath of the Ancients": brush dirt family for ancient loot. */
    public static final DeferredHolder<MobEffect, MobEffect> SNIFFER_SOUL =
            EFFECTS.register("sniffer_soul", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xC8814F));

    /** Chorus flower "Echo of the End": big hits trigger a blink that spends duration. */
    public static final DeferredHolder<MobEffect, MobEffect> ECHO =
            EFFECTS.register("echo", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xA59BB8));

    /**
     * Empty shell effect used by every marker registration above. The vanilla
     * {@link MobEffect} constructor is protected, so a public subclass is the
     * sanctioned way to instantiate a no-logic effect from another package.
     */
    public static class MarkerEffect extends MobEffect {
        public MarkerEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private ModEffects() {
    }

    /** Called from the mod constructor with the mod event bus. */
    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    // ==================================================================
    // Ticking / instant effect implementations
    // ==================================================================

    /** Instant: removes every harmful effect currently on the drinker. */
    public static class PurgeEffect extends MobEffect {
        public PurgeEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xF5F0C8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // vanilla default is false, which would never fire this effect
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            for (MobEffectInstance instance : List.copyOf(entity.getActiveEffects())) {
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    entity.removeEffect(instance.getEffect());
                }
            }
            return true;
        }
    }

    /** Instant: every monster that currently targets the drinker forgets them. */
    public static class FadeawayEffect extends MobEffect {
        public FadeawayEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xD8D8C8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // vanilla default is false, which would never fire this effect
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            // 24 blocks covers anything that is actually pathing towards you.
            for (Monster monster : entity.level().getEntitiesOfClass(Monster.class,
                    entity.getBoundingBox().inflate(24.0))) {
                if (monster.getTarget() == entity) {
                    monster.setTarget(null);
                }
            }
            // Phantoms are FlyingMobs, not Monsters, so they need their own sweep.
            for (Phantom phantom : entity.level().getEntitiesOfClass(Phantom.class,
                    entity.getBoundingBox().inflate(24.0))) {
                if (phantom.getTarget() == entity) {
                    phantom.setTarget(null);
                }
            }
            return true;
        }
    }

    /**
     * Blue orchid "First Bloom": the maxim is literal - "Coffee is our
     * bread." The cup itself IS the meal: the first sip lands exactly what a
     * bread would (5 hunger, 6 saturation; refreshes with every cup). While
     * the bloom lasts, food eaten restores 50% more (that bonus lives in
     * FloraEvents#onItemUseFinish), and a well-fed drinker - food bar at
     * vanilla's regeneration line (18) or above - keeps a quiet Haste I:
     * you reap what the bloom feeds.
     */
    public static class TastebloomEffect extends MobEffect {
        public TastebloomEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xF7E3A1);
        }

        @Override
        public void onEffectStarted(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide || !(entity instanceof Player player)) {
                return;
            }
            player.getFoodData().eat(5, 6.0F);
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
            if (player.getFoodData().getFoodLevel() >= 18) {
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false));
            }
            return true;
        }
    }

    /**
     * Poppy aura: the drowsiness is a continuously refreshed aura - hostiles
     * that wander INTO the 7 block radius while you walk away pick it up too.
     * Slowed to a crawl (they still chase - they just cannot catch up),
     * phantoms drop their lock. Drinking NEVER touches insomnia: phantoms
     * keep spawning exactly as vanilla intends - the aura only makes the
     * ones hunting you lose interest for as long as it plays. Each cup adds
     * a fresh 180 seconds ON TOP of whatever aura time is still running, so
     * a whole pot (9 cups) can be stockpiled for one long night.
     */
    public static class DrowsyAuraEffect extends MobEffect {

        /** Seconds of aura one cup pours in; cups stack additively. */
        private static final int CUP_SECONDS = 180;

        /**
         * [gameTime of last cup, total aura duration after it], per drinker.
         * Vanilla's refresh rule only keeps the LONGER duration, so cups do
         * not stack by themselves; between drinks the remaining time is
         * extrapolated from this clock (duration decrements 1 per tick).
         */
        private static final Map<UUID, long[]> LAST_CUP = new HashMap<>();

        /** Guards the nested addEffect below against re-entering this hook. */
        private static boolean stacking;

        public DrowsyAuraEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9C8FB8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0; // refresh the aura twice per second
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            Level level = entity.level();
            for (Monster monster : level.getEntitiesOfClass(Monster.class,
                    entity.getBoundingBox().inflate(7.0))) {
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            }
            // Phantoms are FlyingMobs, not Monsters, so they need their own sweep.
            for (Phantom phantom : level.getEntitiesOfClass(Phantom.class,
                    entity.getBoundingBox().inflate(7.0))) {
                if (phantom.getTarget() == entity) {
                    phantom.setTarget(null);
                }
            }
            return true;
        }

        /**
         * Vanilla fires this hook on every successful addEffect - new cups AND
         * refreshes while the aura still plays - which is exactly "once per
         * cup". By hook time vanilla has already applied its longer-duration
         * rule, so the pre-drink remainder is reconstructed from LAST_CUP and
         * the full cup is re-poured on top via a guarded nested addEffect.
         */
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

    /**
     * Pink tulip: the drinker is carried by a petal film on the water
     * surface. The feet are pinned a hair above the fluid, so the hitbox
     * never touches water and vanilla applies ordinary LAND physics - full
     * walking speed, sprinting and jumping all work on top of the water.
     * The film forms only on the TOPMOST water layer (air above it); inside
     * the body of water the effect stands back entirely. A plunge from the
     * air (falling faster than a stride jump lands) sinks straight through
     * into the water to swim; sneaking sinks through deliberately; a
     * submerged drinker resurfaces smoothly once they rise back to the top.
     *
     * <p><b>Landings and resurfacing are MOTION CLAMPS, never teleports:</b>
     * the effect tick runs before {@code aiStep}/{@code travel}, so the
     * vertical velocity is rewritten to make {@code Entity#move} settle the
     * feet exactly on the film - the way a block collision stops a fall. A
     * teleport-pin instead lets the feet sink below the surface for a frame,
     * which flickers the touching-water state on and silently converts every
     * held-space landing into a swim stroke instead of a jump (see
     * {@code LivingEntity#aiStep}'s fluid jump branch), and shoves a rising
     * swimmer up in one visible jolt. Pink petals drift off the film the
     * whole time it lasts, and every stride splashes water up through it.</p>
     *
     * <p><b>View bob:</b> the film keeps the hitbox off the ground, so
     * {@code Player#aiStep}'s bob branch sees {@code onGround == false} and
     * the camera would be rock steady. {@link FloraEvents} re-applies the
     * vanilla on-land bob formula after that update, so striding on the
     * film sways the view exactly like walking on land.</p>
     */
    public static class PetalWalkEffect extends MobEffect {
        /**
         * Wall-clock gate for the splash footsteps: never more than four per
         * second per entity, whatever the server tick rate is doing - a
         * per-tick sound can exhaust the 247-handle client sound pool when
         * the tick rate is raised for testing.
         */
        private static final Map<UUID, Long> SPLASH_CLOCK = new HashMap<>();

        /** Frees the splash gate when the entity leaves the game. */
        public static void clearPlayer(UUID uuid) {
            SPLASH_CLOCK.remove(uuid);
        }

        /**
         * Descending faster than this (blocks per tick) is a plunge from the
         * air: the film refuses to engage and the faller sinks into the water.
         *
         * <p>The value sits in the gap between the two cases it must tell
         * apart, measured from vanilla physics (v' = (v - 0.08) * 0.98 per
         * tick, jump velocity 0.42, apex 1.2522): a stride jump made FROM the
         * film is first checkable at -0.514/tick once back inside the catch
         * window, while anything arriving from elsewhere is already faster -
         * a jump off a one-block shore lands at -0.652, a two-block drop at
         * -0.585. So 0.55 keeps every jump made on the water landing back on
         * the film, while every leap or drop arriving from outside sinks
         * through it. Only a gentle one-block step-off (-0.447) still boards
         * the film, the way walking onto water should.</p>
         */
        public static final double PLUNGE_SPEED = 0.55;

        public PetalWalkEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xEBA2C8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // strides need per-tick petal and splash cadence
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            // The film is real geometry now: PetalFilmMixin lends the water
            // block under the drinker a collision box, so boarding a bank,
            // wading ashore and jumping in place are plain vanilla physics -
            // no pin, no teleport, nothing for the engine to fight. This
            // tick only decorates the film with petals and splash.
            Level level = entity.level();
            if (level.isClientSide()) {
                return true;
            }
            // Splashes belong on the film: on dry land the effect is silent.
            BlockPos under = BlockPos.containing(entity.getX(), entity.getY() - 0.15, entity.getZ());
            if (!entity.onGround() || !level.getFluidState(under).is(FluidTags.WATER)) {
                return true;
            }
            ServerLevel server = (ServerLevel) level;
            // Player movement is client authoritative: on the server the
            // real step is read from the position diff (deltaMovement
            // stays ~0 for players); non-player mobs keep using motion.
            double strideDist = entity instanceof Player walker
                    ? Math.hypot(FloraEvents.lastMove(walker)[0], FloraEvents.lastMove(walker)[1])
                    : entity.getDeltaMovement().horizontalDistance();
            boolean striding = strideDist > 0.01;
            if (striding) {
                // A stride kicks petals off the film while water splashes
                // up through it around every step.
                if (entity.getRandom().nextInt(2) == 0) {
                    server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                            entity.getX(), entity.getY() + 0.1, entity.getZ(), 2, 0.3, 0.02, 0.3, 0.01);
                }
                server.sendParticles(ParticleTypes.SPLASH,
                        entity.getX(), entity.getY() + 0.1, entity.getZ(), 8, 0.4, 0.1, 0.4, 0.05);
                server.sendParticles(ParticleTypes.FALLING_WATER,
                        entity.getX(), entity.getY() + 0.4, entity.getZ(), 2, 0.3, 0.05, 0.3, 0.0);
                // Footsteps on water: a light splash at most every 250ms,
                // gated by wall clock rather than ticks so an accelerated
                // tick rate cannot flood the 247-handle client sound
                // pool with one sound per tick.
                Long lastSplash = SPLASH_CLOCK.get(entity.getUUID());
                if (lastSplash == null || System.currentTimeMillis() - lastSplash >= 250L) {
                    SPLASH_CLOCK.put(entity.getUUID(), System.currentTimeMillis());
                    level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_SPLASH,
                            SoundSource.PLAYERS, 0.12f, 1.0f + entity.getRandom().nextFloat() * 0.3f);
                }
            } else if (entity.getRandom().nextInt(4) == 0) {
                // Standing still: petals just keep drifting off the film.
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        entity.getX(), entity.getY() + 0.1, entity.getZ(), 2, 0.3, 0.02, 0.3, 0.01);
            }
            return true;
        }
    }

    /**
     * Oxeye daisy divination: every 8 seconds the daisy is plucked again -
     * heal a heart, quiet the stomach, a short burst of speed, or nothing.
     */
    public static class DivinationEffect extends MobEffect {
        public DivinationEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xEFE9C0);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 160 == 0; // one pluck every 8 seconds
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                // Server and client roll different randoms; running the roll on
                // both would leave local speed/food effects the server never
                // synced, lingering in the HUD after the effect ends.
                return true;
            }
            switch (entity.getRandom().nextInt(4)) {
                case 0 -> entity.heal(2.0f); // loves me: a heart back
                case 1 -> {
                    if (entity instanceof Player player) {
                        player.getFoodData().eat(1, 0.4f); // loves me: half a shank
                    }
                }
                case 2 -> entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
                default -> { // loves me not: nothing at all
                }
            }
            return true;
        }
    }

    /**
     * Wither rose aura: everything alive within 7 blocks withers away and is
     * wrapped in dark motes - the drinker alone is spared (they already paid
     * 2 seconds of wither as the price of the drink).
     */
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
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(7.0))) {
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

    /**
     * Sunflower duality: under the open day sky the drinker slowly mends;
     * at night they shine - the flower keeps glowing for the extinguished
     * sun (dynamic light via the SodiumDynamicLights integration).
     */
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
                // Refreshed every second while the sun finds you.
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false));
                // "夸父逐日" (Kuafu Chases the Sun): the bookkeeping only runs
                // while the day form is actually active - clouds, night or a
                // roof between the drinker and the sky pause the chase.
                if (entity instanceof ServerPlayer player) {
                    FloraAdvancements.sunwardDayForm(player);
                }
            } else if (!level.isDay()) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
            }
            return true;
        }
    }

    /**
     * Spore blossom: crops in a true cube of 3 blocks radius around the
     * drinker (7x7x7, player-centered) ripen randomly, like a permanent
     * drizzle of bone meal wherever they stand.
     */
    public static class SproutEffect extends MobEffect {
        public SproutEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x7BBF4A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 10 == 0; // scan twice per second
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            ServerLevel server = (ServerLevel) entity.level();
            BlockPos center = entity.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                BlockState state = server.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                    if (entity.getRandom().nextFloat() < 0.25f) {
                        int age = state.getValue(CropBlock.AGE);
                        server.setBlock(pos, state.setValue(CropBlock.AGE, age + 1), 2);
                        // "相信整个春天" (Believe in the Whole Spring): every
                        // ripened crop counts toward fifty per effect.
                        if (entity instanceof ServerPlayer player) {
                            FloraAdvancements.sproutCropRipened(player);
                        }
                    }
                }
            }
            return true;
        }
    }

    /**
     * Eyeblossom: every living thing inside 25 blocks is made to glow, and
     * whatever the drinker looks STRAIGHT AT (unobstructed line of sight
     * inside a ~15 degree cone) is pinned by the gaze: the first 4 seconds
     * of continuous eye contact freeze the target in place (slowness VII
     * zeroes ground movement), after that it may move again but stays
     * slowed for as long as the stare holds. Break the stare - or let the
     * effect lapse - and the tally resets to zero.
     */
    public static class GazeEffect extends MobEffect {

        /** Continuous stare ticks before the target may move again (4 s). */
        private static final int FREEZE_TICKS = 80;

        /** No gaze tick for this long means a fresh activation: tallies reset. */
        private static final long STALE_GAP_TICKS = 5;

        /** Continuous stare ticks per (drinker, target) pair. */
        private static final Map<UUID, Map<UUID, Integer>> STARE = new HashMap<>();

        /** Game time of each drinker's last gaze tick, for the staleness reset. */
        private static final Map<UUID, Long> STARE_CLOCK = new HashMap<>();

        /** Frees stare tracking when a player leaves. */
        public static void clearPlayer(UUID uuid) {
            STARE.remove(uuid);
            STARE_CLOCK.remove(uuid);
        }

        public GazeEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9FB6D9);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // the stare must be followed tick by tick
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
                stares.clear(); // fresh activation of the gaze: pinning restarts
            }
            STARE_CLOCK.put(drinker, now);

            Set<UUID> stared = new HashSet<>();
            Vec3 eye = entity.getEyePosition();
            Vec3 look = entity.getViewVector(1.0F);
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(25.0))) {
                if (other == entity) {
                    continue;
                }
                // The glow itself refreshes twice a second, continuous but cheap.
                if (now % 10 == 0) {
                    other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, true, false));
                }
                // Pinned only when the eyes are locked straight on it, with
                // nothing in between.
                Vec3 toOther = other.getEyePosition().subtract(eye);
                double distance = toOther.length();
                boolean seen = distance < 0.5
                        || (look.dot(toOther.normalize()) >= 0.966 && entity.hasLineOfSight(other));
                if (!seen) {
                    continue;
                }
                stared.add(other.getUUID());
                // "凝视深渊" (Gaze into the Abyss): pinning an Enderman is
                // the maxim made literal - the abyss that cannot stare back.
                if (other instanceof EnderMan && entity instanceof ServerPlayer starer) {
                    FloraAdvancements.award(starer, FloraAdvancements.EVENT_GAZE_ABYSS);
                }
                int ticks = stares.getOrDefault(other.getUUID(), 0) + 1;
                stares.put(other.getUUID(), ticks);
                if (ticks < FREEZE_TICKS) {
                    // Held rigid: slowness VII zeroes all ground movement.
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6, true, true));
                } else {
                    // Past the first four seconds the stare only drags.
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
                }
            }
            // A stare that wandered resets - pinning must be continuous.
            stares.keySet().retainAll(stared);
            return true;
        }
    }

    /**
     * Golden dandelion: the flower opens into ONE random tier-II blessing -
     * strength, speed, resistance or haste - or plain luck, for three
     * minutes.
     */
    public static class WishEffect extends MobEffect {
        public WishEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xFFD700);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // vanilla default is false, which would never fire this effect
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) {
                return true;
            }
            MobEffectInstance[] pool = {
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3 * 60 * 20, 1),
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3 * 60 * 20, 1),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3 * 60 * 20, 1),
                    new MobEffectInstance(MobEffects.DIG_SPEED, 3 * 60 * 20, 1),
                    new MobEffectInstance(MobEffects.LUCK, 3 * 60 * 20, 0)};
            entity.addEffect(pool[entity.getRandom().nextInt(pool.length)]);
            if (entity.level() instanceof ServerLevel server) {
                server.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.0F), 0.9F),
                        entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
            }
            return true;
        }
    }

    /**
     * Wildflowers: the walk becomes a paving walk. While moving, a sheet-like
     * block held in the main hand (anything in the
     * {@code kaleidoscope_flora:sheet_blocks} tag - wool carpets, moss
     * carpets, snow layers, pink petals, wildflowers, leaf litter...) is laid
     * underfoot automatically, spending one item per block. Walking a room
     * carpets it; walking a hillside traces a path - the builder's drink.
     */
    public static class FlowerPathEffect extends MobEffect {

        /** The wildflowers block (VanillaBackport) bloomed underfoot; resolved lazily. */
        private static Block wildflowersBlock;

        /** The block each player currently stands on, for dwell-time tracking. */
        private static final Map<UUID, BlockPos> STAND_POS = new HashMap<>();

        /** How many ticks the player has stood on that block. */
        private static final Map<UUID, Integer> STAND_TICKS = new HashMap<>();

        /** Ticks of standing per extra flower on a block (1s each, up to 4). */
        private static final int TICKS_PER_FLOWER = 20;

        /** Frees dwell tracking when a player logs out. */
        public static void clearPlayer(UUID uuid) {
            STAND_POS.remove(uuid);
            STAND_TICKS.remove(uuid);
        }

        public FlowerPathEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9CDF6A);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // the trail must follow every step the player takes
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
            // Player movement is client authoritative: on the server the real
            // per-tick step comes from the position diff, not deltaMovement.
            double[] step = FloraEvents.lastMove(player);
            double dist = Math.hypot(step[0], step[1]);
            Level level = player.level();

            // Dwell time on the current feet block decides how many flowers it
            // ends up holding: passed by = 1, each second stood adds one, 4 max.
            UUID uuid = player.getUUID();
            BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
            int dwell = feet.equals(STAND_POS.get(uuid)) ? STAND_TICKS.getOrDefault(uuid, 0) + 1 : 0;
            STAND_POS.put(uuid, feet);
            STAND_TICKS.put(uuid, dwell);
            growFlowers(level, feet, 1 + Math.min(3, dwell / TICKS_PER_FLOWER));

            if (dist < 0.02) {
                // Standing still: the walk still blooms underfoot when the
                // hand offers no sheet block (held sheets only lay in motion).
                ItemStack held = player.getMainHandItem();
                if (!isSheetItem(held)) {
                    layWildflowers(level, feet);
                }
                return true;
            }

            // Sample the line walked this tick finely enough that not a
            // single block of the trail is skipped, even at sprint speed.
            ItemStack held = player.getMainHandItem();
            BlockItem blockItem = isSheetItem(held)
                    ? (BlockItem) held.getItem()
                    : null;
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

        /**
         * True when the stack's item is a block in the sheet-blocks tag. The
         * tag only loads when placed under {@code tags/block/} (singular) on
         * 1.21+ - a wrong folder silently yields an empty tag, which once made
         * held carpets fall through to the wildflowers fallback.
         */
        private static boolean isSheetItem(ItemStack held) {
            return held.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(ModTags.SHEET_BLOCKS);
        }

        /**
         * Enriches the flower block underfoot up to the target amount: any
         * block carrying the {@code flower_amount} property (pink petals,
         * VanillaBackport wildflowers) grows by one per second the player
         * lingers on it, capped at the property's maximum (4).
         */
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

        /**
         * Blooms wildflowers underfoot when the hand is empty or holds a
         * non-sheet item - the walk flowers by itself, spending nothing. The
         * drink only exists when VanillaBackport is installed (it brews from
         * its wildflowers), so the block resolves to itself.
         */
        private static void layWildflowers(Level level, BlockPos pos) {
            if (wildflowersBlock == null) {
                wildflowersBlock = BuiltInRegistries.BLOCK.get(
                        ResourceLocation.withDefaultNamespace("wildflowers"));
            }
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
            SoundType sound = state.getSoundType(level, pos, null);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, 0.7f, 1.0f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.15, 0.3, 0.02);
            }
        }

        /**
         * Lays one held sheet block at the given feet position. The spot must
         * be replaceable and dry; the state used is exactly what hand-placing
         * the item would produce, so petals orient, snow layers stack and
         * carpets sit the way the player expects.
         */
        private static void laySheet(Level level, Player player, ItemStack held, BlockItem blockItem, BlockPos pos) {
            if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
                return;
            }
            // Mirror a hand placement: "clicked" the top face of the block below.
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
}
