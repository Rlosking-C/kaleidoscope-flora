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

/**
 * All 24 custom MobEffects of Kaleidoscope Flora.
 *
 * <p>Two flavours of effects live here:</p>
 * <ul>
 *   <li><b>Ticking / instant effects</b> (12 classes below) - they carry their
 *       logic inside {@code applyEffectTick} (auras, water walking, crops,
 *       day/night switching, air swimming, random boons...).</li>
 *   <li><b>Marker effects</b> (12 further registrations) - most are plain
 *       markers with no code of their own. They only exist so
 *       {@code hasEffect(...)} can be checked inside {@link FloraEvents},
 *       which implements their behaviour through damage / interact / loot
 *       events (thorns, vampirism, looting, petal veil...).</li>
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

    /**
     * Scales a duration in seconds by {@code effectDurationMultiplier}, in ticks.
     *
     * <p>The twin of {@code FloraDrinks.ticks(int)} - that one is private and
     * this file needs the same maths for the one effect that hands out a
     * <em>vanilla</em> effect with its own duration ({@link WishEffect}).
     * Keep the two in step: both must apply the config multiplier, and both
     * must fall back to 1.0 when the config is not loaded yet.</p>
     *
     * <p><b>Why the fallback matters.</b> Effect instances can be built during
     * item registration, long before the COMMON config file is read. Reading
     * {@code effectDurationMultiplier()} at that moment throws, so the
     * {@code isLoaded()} guard is not defensive padding - it is what keeps
     * registration working at all.</p>
     */
    static int scaledTicks(int seconds) {
        double multiplier = FloraConfig.SPEC.isLoaded()
                ? FloraConfig.effectDurationMultiplier()
                : 1.0;
        return Math.max(1, (int) (seconds * 20 * multiplier));
    }

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

    /** Mooncake "Floating": the eater swims through the air. */
    public static final DeferredHolder<MobEffect, FloatingEffect> FLOATING =
            EFFECTS.register("floating", FloatingEffect::new);

    // ------------------------------------------------------------------
    // v0.3.4 flower cakes
    // ------------------------------------------------------------------

    /**
     * Toasted Flower Cake: attack and movement speed up, and hunger drains
     * twice as fast.
     *
     * <p>A marker: the speed halves are two ordinary vanilla effects applied
     * alongside it by {@code FlowerCakeItem}, the doubled hunger drain lives in
     * {@link FloraEvents#onPlayerTick}, and the sluggish comedown is applied
     * when this effect expires. There is no per-tick logic of its own, so a
     * {@link MobEffect} subclass would have been pure ceremony.</p>
     *
     * <p><b>Why the penalty is a vanilla Slow rather than a custom effect:</b>
     * the point is legibility - the player should be able to look at their
     * effect bar and understand what happened without reading a tooltip.</p>
     */
    public static final DeferredHolder<MobEffect, MobEffect> TOASTED_CAKE =
            EFFECTS.register("toasted_cake", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xD9A441));

    /**
     * Dew Flower Cake: an extra mid-air jump, and a shockwave on landing from
     * height.
     *
     * <p>A marker as well, but a load-bearing one - three separate places ask
     * "does this entity have the dew cake up?" and none of them is a tick
     * handler:</p>
     * <ul>
     *   <li>the extra jump is counted in
     *       {@code mixin.client.DoubleJumpLocalPlayerMixin}, which reads the
     *       <b>amplifier</b> as "how many extra jumps";</li>
     *   <li>fall damage immunity and the landing shockwave are in
     *       {@link FloraEvents};</li>
     *   <li>the shockwave's four-tier curve is
     *       {@code FloraEvents.DewImpact}.</li>
     * </ul>
     *
     * <p>Amplifier 0 = one extra jump (a literal double jump), matching the
     * "次数 = 放大器 + 1" contract the mixin documents.</p>
     */
    public static final DeferredHolder<MobEffect, MobEffect> DEW_CAKE =
            EFFECTS.register("dew_cake", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x8FD8E8));

    /**
     * Raw Flower Cake hit: the target is held in place for one second.
     *
     * <p>A marker, because "pinned" is three separate mechanisms and this is the
     * one name that ties them together:</p>
     * <ul>
     *   <li>{@code MOVEMENT_SLOWDOWN} VI and {@code WEAKNESS} are applied next
     *       to this effect by {@link FloraEvents#onProjectileImpact} - they stop
     *       the walking and the hitting;</li>
     *   <li>this effect is what {@code mixin.PinJumpMixin} looks for to refuse
     *       the jump, since that is the only third with no other hook (see that
     *       class for why).</li>
     * </ul>
     *
     * <p><b>Why a marker of our own instead of just checking the slow.</b> The
     * mixin has to distinguish "pinned by a cake" from "happens to be slowed by
     * something else" - otherwise every mob in the world that walked into a
     * cobweb or got splashed by a turtle would stop jumping. Checking our own
     * effect is the cheap, exact answer.</p>
     *
     * <p>Deliberately <b>HARMFUL</b>: it is a debuff, so Absolution's charge
     * gate and Purge's clear-3 both see it as one, and neither can be used to
     * shrug off the pin for free.</p>
     *
     * <p>Unlike the other markers this one <b>ticks</b>, because the pin needs a
     * visible state while it lasts - see {@link PinnedEffect}.</p>
     */
    public static final DeferredHolder<MobEffect, MobEffect> PINNED =
            EFFECTS.register("pinned", PinnedEffect::new);

    /**
     * How long a Raw Flower Cake pins its target, in ticks.
     *
     * <p>Lives here rather than in {@code FloraEvents} because two places have to
     * agree on it now: the code that applies the pin, and the effect that draws
     * the pin's ring and needs to know how far through it is.</p>
     */
    public static final int PIN_TICKS = 20;

    // ------------------------------------------------------------------
    // Pin ring geometry
    // ------------------------------------------------------------------
    //
    // Shared by the hit burst (FloraEvents.playPinImpact) and the pin's own tick
    // (PinnedEffect), so the moment of the hit and the state that follows it are
    // drawn at the same size. All of it is derived from the target's hitbox:
    // a fixed radius looks like a bracelet on a player and like a speck on a
    // spider.

    /** Smallest ring radius, so tiny mobs still get a visible circle. */
    private static final double PIN_MIN_RADIUS = 0.45;

    /** Ring radius as a fraction of the target's width (half-width is 0.5). */
    private static final double PIN_RADIUS_PER_WIDTH = 0.75;

    /** Target spacing between neighbouring particles, in blocks. */
    private static final double PIN_PARTICLE_SPACING = 0.4;

    private static final int PIN_MIN_PARTICLES = 6;

    /**
     * Ceiling on particles per tick, for cost rather than for looks.
     *
     * <p>Each particle is its own {@code sendParticles} call, because a ring has
     * to be points on a circle and the spread-box overload would scatter them
     * instead - so this number is also the packet count per tick. At 48 the
     * largest useful cases still get a continuous ring: a ghast (4 blocks wide,
     * radius 3.0) works out at 47 points and just fits under the cap. An ender
     * dragon (16 wide, radius 12) would want ~190 and gets a sparse ring
     * instead, which is the deliberate trade: pinning a dragon with a cake is an
     * edge case, and a 190-point ring every tick for a second is not worth
     * paying for it.</p>
     */
    private static final int PIN_MAX_PARTICLES = 48;

    /** Where the ring sits vertically, as a fraction of the target's height. */
    private static final double PIN_LIFT_PER_HEIGHT = 0.35;
    private static final double PIN_MIN_LIFT = 0.15;
    private static final double PIN_MAX_LIFT = 0.6;

    /**
     * Radius of the pin's ring around this target, in blocks.
     *
     * <p>Scales with the hitbox width and then some, so the ring encircles the
     * silhouette rather than lying on top of it.</p>
     */
    public static double pinRingRadius(LivingEntity entity) {
        return Math.max(PIN_MIN_RADIUS, entity.getBbWidth() * PIN_RADIUS_PER_WIDTH);
    }

    /**
     * How many particles to place on a ring of this radius.
     *
     * <p>Derived from the circumference, not fixed: a big mob's ring is several
     * blocks around, and a constant count would spread six petals so far apart
     * that they stop reading as a ring at all.</p>
     */
    public static int pinRingCount(double radius) {
        int count = (int) Math.round(radius * 2.0 * Math.PI / PIN_PARTICLE_SPACING);
        return Math.max(PIN_MIN_PARTICLES, Math.min(PIN_MAX_PARTICLES, count));
    }

    /**
     * Height above the target's feet at which the ring starts, in blocks.
     *
     * <p>Clamped at the top: the ring is meant to read as "held at the feet", so
     * on a tall mob it should not climb to the head.</p>
     */
    public static double pinRingLift(LivingEntity entity) {
        return Math.max(PIN_MIN_LIFT, Math.min(PIN_MAX_LIFT, entity.getBbHeight() * PIN_LIFT_PER_HEIGHT));
    }

    // ------------------------------------------------------------------
    // Marker effects (no code; behaviour lives in FloraEvents)
    // ------------------------------------------------------------------

    /**
     * The pin's visible state: a slow ring of petals around the target's feet
     * that climbs a little and then hangs there.
     *
     * <p><b>Why this is an effect and not a one-off burst.</b> The hit burst
     * alone (fired from {@code FloraEvents.onProjectileImpact}) says "something
     * landed"; it does not say "you are still held". A one-second debuff the
     * player cannot see reads as lag rather than as a mechanic, which is exactly
     * what testing reported: "when the raw cake hits a creature it should have
     * particles showing it is pinned". This runs for as long as the pin does, so
     * the feedback and the mechanic cannot drift apart - and it stops on its own
     * when the effect is removed, for any reason.</p>
     *
     * <p><b>The rise-then-hover arc</b> uses the effect's own remaining duration
     * rather than a per-entity animation timer: the ring climbs over roughly the
     * first half and then holds for the rest, which needs no state, survives
     * re-application, and cannot get out of step with the pin.</p>
     *
     * <p><b>Every dimension comes from the target's own hitbox</b> - see
     * {@link #pinRingRadius}. The first version hard-coded a 0.45 block ring,
     * which is a sensible bracelet on a player and sits <em>inside</em> a spider,
     * whose hitbox is 1.4 blocks wide; testing reported exactly that ("the
     * particles should suit the creature - for a spider they are too small").
     * Sizing to the hitbox means the ring encircles the mob instead of appearing
     * on its back.</p>
     *
     * <p>Server-side only. Sent as particles rather than spawned entities, so a
     * pinned mob costs nothing but a packet.</p>
     */
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
            // Two things had to be true at once for this to read as a ring at
            // all, and the first two attempts each got one of them wrong.
            //
            // 1. THE ANGLES MUST NOT ROTATE. Rotating them by 0.35 rad per tick
            //    sweeps more than a full turn over the pin, so no angle is ever
            //    reinforced and the petals scatter instead of forming a circle.
            //    Fixed angles are kept.
            //
            // 2. THE HEIGHT MUST NOT CLIMB, and the particle must not fall.
            //    Fixing the angles alone traded one artefact for a worse one: a
            //    rising ring deposits its ten ticks at the same angle at ten
            //    different heights, which stacks them into vertical dashes - and
            //    the particle being used (CHERRY_LEAVES, i.e. CherryParticle,
            //    which overrides tick() and falls) added a long downward trail on
            //    top. On a big mob the dashes stood apart and the report was
            //    "still scattered columns"; on a small one they merged and it
            //    looked fine, which is why this took three passes to see.
            //
            // So: a fixed height, and ParticleTypes.END_ROD, whose particle
            // class overrides no tick() and sets no gravity - it simply hovers
            // where it is put. That is also the honest picture of a target held
            // still.
            //
            // Deviation from the design's "rises slowly, then hovers": the rise
            // is gone. A ring that rises while its angles are fixed cannot read
            // as a ring; the two requirements are in direct conflict and the ring
            // was what testing asked for. The end-rod's own fade supplies the
            // only motion. It is also ice-white, matching the pin's palette (the
            // hit burst is CRIT, the effect colour is pale blue) rather than the
            // pink the falling petal had.
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

    /** Blue orchid "First Bloom": eaten food restores 50% more nutrition. */
    public static final DeferredHolder<MobEffect, MobEffect> TASTEBLOOM =
            EFFECTS.register("tastebloom", TastebloomEffect::new);

    /** Allium "Fire Waltz": melee hits ignite the target. */
    public static final DeferredHolder<MobEffect, MobEffect> FIREBRAND =
            EFFECTS.register("firebrand", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE86A17));

    /** Red tulip "Crimson Heartbeat": 15% of melee damage returns as health. */
    public static final DeferredHolder<MobEffect, MobEffect> VAMPIRIC =
            EFFECTS.register("vampiric", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xB02E26));

    /**
     * Orange tulip "Autumn Serenade": breaking mature crops multiplies the
     * drops (x2-x4), and farmland is not destroyed underfoot - the harvest
     * walk leaves the soil as it found it (see
     * {@link FloraEvents#onFarmlandTrample}).
     */
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

    /**
     * True while the entity drifts on the mooncake's effect AND is off the
     * ground. On the ground the drinker walks, jumps and runs like anyone
     * else - only the air is turned into water (the reference effect from
     * Kaleidoscope End slows land movement instead; this does not).
     */
    public static boolean isAirborne(LivingEntity entity) {
        return !entity.onGround() && entity.hasEffect(FLOATING);
    }

    // ==================================================================
    // Ticking / instant effect implementations
    // ==================================================================

    /** Instant: removes up to {@link #PURGE_MAX_EFFECTS} harmful effects. */
    public static class PurgeEffect extends MobEffect {

        /**
         * How many harmful effects one cup clears (v0.3.4).
         *
         * <p>The drink used to wipe the whole list, which made it a hard counter
         * to every debuff in the game including stacked ones from bosses and
         * other mods. Three is enough to break a bad moment - a wither hit, a
         * poison, a slow - without erasing an entire encounter's worth of
         * punishment. Which three is decided by {@code getActiveEffects}' own
         * ordering; that is not worth sorting, since the player cannot choose
         * and any deterministic subset is as fair as another.</p>
         */
        static final int PURGE_MAX_EFFECTS = 3;

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
            double range = 24.0 * FloraConfig.auraRangeMultiplier();
            // 24 blocks covers anything that is actually pathing towards you.
            for (Monster monster : entity.level().getEntitiesOfClass(Monster.class,
                    entity.getBoundingBox().inflate(range))) {
                if (monster.getTarget() == entity) {
                    monster.setTarget(null);
                }
            }
            // Phantoms are FlyingMobs, not Monsters, so they need their own sweep.
            for (Phantom phantom : entity.level().getEntitiesOfClass(Phantom.class,
                    entity.getBoundingBox().inflate(range))) {
                if (phantom.getTarget() == entity) {
                    phantom.setTarget(null);
                }
            }
            return true;
        }
    }

    /**
     * Blue orchid "First Bloom": food eaten while the bloom lasts restores 20%
     * more, and a well-fed drinker keeps a quiet Haste I.
     *
     * <p><b>v0.3.4 changed this drink twice.</b> The bonus was halved
     * (50% -&gt; 20%), the Haste threshold was raised from vanilla's
     * regeneration line to a completely full bar, and then the opening meal was
     * <b>removed entirely</b>: the cup used to land a bread's worth of hunger on
     * the first sip (and on every sip before that), which made it a food item
     * that happened to be a potion. It is now purely a food <em>multiplier</em> -
     * it makes eating better rather than replacing eating, which is what the
     * "you reap what the bloom feeds" line was always about.</p>
     *
     * <p>Nothing is applied on {@code onEffectStarted} any more, so this class
     * has no start hook. The 20% lives in
     * {@code FloraEvents#onItemUseFinish}, and the Haste is per-tick below.</p>
     */
    public static class TastebloomEffect extends MobEffect {

        /**
         * Food level at which the bloom grants Haste I.
         *
         * <p>v0.3.4: raised 18 -&gt; 20, i.e. from vanilla's regeneration line to
         * a completely full bar. At 18 the buff was up for most of a normal play
         * session, which made it background noise; at 20 it only pays out when
         * the player has actually eaten - which is also what keeps this drink
         * meaningful now that it no longer feeds anyone by itself.</p>
         */
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
            double range = 7.0 * FloraConfig.auraRangeMultiplier();
            for (Monster monster : level.getEntitiesOfClass(Monster.class,
                    entity.getBoundingBox().inflate(range))) {
                monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            }
            // Phantoms are FlyingMobs, not Monsters, so they need their own sweep.
            for (Phantom phantom : level.getEntitiesOfClass(Phantom.class,
                    entity.getBoundingBox().inflate(range))) {
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

    /**
     * Sunflower duality: under the open day sky the drinker slowly mends;
     * at night they shine - the flower keeps glowing for the extinguished
     * sun (dynamic light via the SodiumDynamicLights or LambDynamicLights
     * integration, whichever is installed).
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
     * Spore blossom: crops in a true cube of 5 blocks radius around the
     * drinker (11x11x11, player-centered) ripen randomly, like a permanent
     * drizzle of bone meal wherever they stand.
     *
     * <p><b>Two growth paths.</b> Vanilla crops (wheat, carrots, ...) are
     * {@link CropBlock}s and get exactly one age step, as before. Everything
     * else has to go through {@link BonemealableBlock}, and Farmer's Delight's
     * rice is why that branch exists: {@code RiceBlock extends BushBlock}, so
     * the plain {@code instanceof CropBlock} test walked straight past it and
     * the effect did nothing at all for rice (player report, 2026-09-23). Going
     * through the block's own {@code performBonemeal} means a modded crop grows
     * by its own rules instead of us poking a property by hand.</p>
     *
     * <p>Farmer's Delight's rice needs BOTH paths, which is how the two reports
     * on it (2026-09-23) arose: the base {@code rice} is a
     * {@link BonemealableBlock} that is not a {@link CropBlock} at all, while
     * the grain head above it, {@code rice_panicles}, IS a {@link CropBlock} but
     * carries an age property of its own instead of {@code CropBlock.AGE}. The
     * first report was the effect doing nothing for rice; the second was it
     * throwing on the panicles. Both are handled by {@code ripenOne}.</p>
     *
     * <p>The integer {@code age} property test in {@code ripenOne} is deliberate:
     * {@link BonemealableBlock} is also implemented by grass, saplings and other
     * non-crops, and a spore blossom should not start carpeting the world in
     * flowers.</p>
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
                if (entity.getRandom().nextFloat() >= 0.25f) {
                    continue;
                }
                if (!ripenOne(server, pos, entity)) {
                    continue;
                }
                // "相信整个春天" (Believe in the Whole Spring): every ripened
                // crop counts toward fifty per effect.
                if (entity instanceof ServerPlayer player) {
                    FloraAdvancements.sproutCropRipened(player);
                }
            }
            return true;
        }

        /**
         * Advances one crop by a single step. Returns false when the block is not
         * a crop, is already ripe, or its own bonemeal rules refuse to grow it.
         *
         * <p><b>Never assume {@link CropBlock#AGE} is on the state.</b> A
         * {@code CropBlock} is free to carry a property of its own instead:
         * Farmer's Delight's {@code rice_panicles} (the grain head above its
         * rice) extends {@code CropBlock} but swaps the age property for its own
         * {@code rice_age}. Poking {@code CropBlock.AGE} on that state threw
         * straight out of the effect tick - {@code IllegalArgumentException:
         * Cannot get property IntegerProperty{name=age, values=[0..7]} as it does
         * not exist in Block{farmersdelight:rice_panicles}} (player crash,
         * 2026-09-23). The block's own accessor is {@code protected}, so the
         * property cannot simply be asked for.</p>
         *
         * <p>So the one-step shortcut is taken only for a crop that genuinely
         * carries {@code age}; everything else - Farmer's Delight's rice, its
         * rice panicles, and any other modded crop - is grown through the
         * block's own {@link BonemealableBlock} rules, exactly as bone meal
         * would. That keeps each mod's crop growing by its own conventions and
         * leaves no way for this tick to throw.</p>
         */
        private static boolean ripenOne(ServerLevel level, BlockPos pos, LivingEntity entity) {
            BlockState state = level.getBlockState(pos);
            boolean cropLike = state.getBlock() instanceof CropBlock || ageProperty(state) != null;
            if (!cropLike) {
                // Grass, saplings and the other bonemeal-accepting non-crops stay
                // untouched: a spore blossom should not carpet the world.
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

        /**
         * The top of {@link CropBlock#AGE}. The bound is read off the property
         * itself so an increment can never be handed an out-of-range value.
         */
        private static final int MAX_CROP_AGE = CropBlock.AGE.getPossibleValues().stream()
                .mapToInt(Integer::intValue).max().orElse(7);

        /**
         * The integer {@code age} property of a block state, or null when the
         * state has none - which is how actual crops are told apart from the
         * other things that accept bonemeal (grass, saplings, ...).
         */
        private static IntegerProperty ageProperty(BlockState state) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof IntegerProperty age && "age".equals(property.getName())) {
                    return age;
                }
            }
            return null;
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
            double gazeRange = 25.0 * FloraConfig.auraRangeMultiplier();
            for (LivingEntity other : server.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(gazeRange))) {
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
     * strength, speed, resistance or haste - or plain luck, for two
     * minutes (v0.3.4: was hard-coded at three).
     *
     * <p>The WISH effect itself is applied with a duration of 1 tick: it is a
     * delivery vehicle, not a lasting state. {@code applyEffectTick} fires once
     * and hands over a real vanilla effect, so what the player keeps is the
     * blessing below - which means <b>that</b> duration is the one the
     * {@code effectDurationMultiplier} config has to scale. It was the only
     * duration in the whole mod that ignored the multiplier, purely because it
     * lived here as a literal instead of going through
     * {@link #scaledTicks(int)}.</p>
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

        /**
         * Positions where the <b>free</b> bloom put a wildflowers block, per
         * dimension. This is what makes "the walk's flowers give nothing back"
         * actually true.
         *
         * <p><b>Why a position set is needed at all.</b> The first version of
         * the no-drop rule simply asked "is the breaker a player with this
         * effect, holding nothing?", which covers a player breaking the flower
         * directly - but not the case reported in testing: <em>breaking the dirt
         * underneath</em>. A block that loses its support is destroyed by the
         * engine's own neighbour update, and that path carries <b>no breaker at
         * all</b>, so the old filter could not see it and the flower dropped
         * anyway. Only the position tells the two apart, so the positions are
         * remembered.</p>
         *
         * <p>Keyed by dimension rather than by {@code Level} instance: a server
         * in one JVM can load several worlds, and a {@code Level} key would
         * either pin dead worlds or - if reused - leak one world's coordinates
         * into another's. Entries are dropped when the block is destroyed and
         * the whole set is dropped when the dimension unloads.</p>
         */
        private static final Map<ResourceKey<Level>, Set<BlockPos>> TRAIL = new HashMap<>();

        /**
         * Safety valve: if a dimension's trail grows past this (a player who
         * lays thousands and never breaks any), entries whose block is no longer
         * a wildflowers block are swept. Bounded work, no unbounded growth.
         */
        private static final int TRAIL_SWEEP_THRESHOLD = 4096;

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
            ItemStack held = player.getMainHandItem();
            boolean layingFromHand = isSheetItem(held);

            // Dwell time on the current feet block decides how many flowers it
            // ends up holding: passed by = 1, each second stood adds one, 4 max.
            //
            // v0.3.4 fix: the dwell growth runs ONLY for the free bloom, never
            // while the player is laying a stack they are holding. Reported in
            // testing: "holding a wildflower stack, one gets consumed and a
            // second later another one appears out of nothing". That was this
            // growth landing on top of a block the player had just paid for, so
            // the net effect was a free item every second - exactly the loop the
            // no-drop rule was meant to close. Standing on your own traced path
            // still enriches it; paying for a block does not also enrich it.
            UUID uuid = player.getUUID();
            BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
            int dwell = feet.equals(STAND_POS.get(uuid)) ? STAND_TICKS.getOrDefault(uuid, 0) + 1 : 0;
            STAND_POS.put(uuid, feet);
            STAND_TICKS.put(uuid, dwell);
            if (!layingFromHand) {
                growFlowers(level, feet, 1 + Math.min(3, dwell / TICKS_PER_FLOWER));
            }

            if (dist < 0.02) {
                // Standing still: the walk still blooms underfoot when the
                // hand offers no sheet block (held sheets only lay in motion).
                if (!layingFromHand) {
                    layWildflowers(level, feet);
                }
                return true;
            }

            // Sample the line walked this tick finely enough that not a
            // single block of the trail is skipped, even at sprint speed.
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

        /**
         * True when the stack's item is a block in the sheet-blocks tag. The
         * tag only loads when placed under {@code tags/block/} (singular) on
         * 1.21+ - a wrong folder silently yields an empty tag, which once made
         * held carpets fall through to the wildflowers fallback.
         *
         * <p>Package-visible rather than private since v0.3.4: the loot side in
         * {@link FloraEvents} needs the same "is this an empty hand?" answer the
         * laying side uses, and two copies of this predicate would be one copy
         * too many - a tag check that drifts out of step would silently turn
         * the no-drop rule on or off for held carpets.</p>
         */
        static boolean isSheetItem(ItemStack held) {
            return held.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(ModTags.SHEET_BLOCKS);
        }

        /**
         * The wildflowers block this effect blooms, resolved lazily.
         *
         * <p>Comes from VanillaBackport, so it may be absent; the lookup is by
         * registry name and returns {@code Blocks.AIR} when missing, which every
         * caller has to handle. Both the laying code and the no-drop rule in
         * {@link FloraEvents} go through here so they can never disagree about
         * which block "the walk's flowers" means.</p>
         */
        static Block wildflowers() {
            if (wildflowersBlock == null) {
                wildflowersBlock = BuiltInRegistries.BLOCK.get(
                        ResourceLocation.withDefaultNamespace("wildflowers"));
            }
            return wildflowersBlock;
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

        /** Records a free-bloom block so its drops can be suppressed later. */
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

        /**
         * True when this position holds a block the free bloom laid - the
         * question {@code FloraEvents.onWalkFlowerDrops} cannot answer from the
         * event alone, because a block destroyed by its support vanishing has no
         * breaker.
         */
        public static boolean isTrailBlock(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server)) {
                return false;
            }
            Set<BlockPos> trail = TRAIL.get(server.dimension());
            return trail != null && trail.contains(pos);
        }

        /** Forgets a trail position - called once its block is gone. */
        public static void untrackTrail(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            Set<BlockPos> trail = TRAIL.get(server.dimension());
            if (trail != null) {
                trail.remove(pos);
            }
        }

        /** Drops a whole dimension's trail when that dimension unloads. */
        public static void clearTrail(ResourceKey<Level> dimension) {
            TRAIL.remove(dimension);
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

    /**
     * Mooncake: while the moon is in the eater, the air turns into water -
     * no falling, look where you want to go, hold the swim key to rise.
     *
     * <p>The movement is not written here: the engine is convinced instead.
     * Four small mixins (see {@code com.rlosking.flora.mixin}) make NeoForge's
     * EMPTY fluid report a height, make it swimmable, and let the vanilla swim
     * state engage, so {@code LivingEntity#travel} takes its ordinary water
     * branch in mid-air. That approach is lifted from Kaleidoscope End's 梦境
     * effect (the reference implementation for air swimming).</p>
     *
     * <p>This tick only keeps the fall meter at zero: vanilla's fall damage
     * measures the block underfoot, which is still plain air, so a landing
     * after a long drift would otherwise hurt.</p>
     */
    public static class FloatingEffect extends MobEffect {
        public FloatingEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xBBD4F5);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true; // the meter has to be cleared before every landing
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
