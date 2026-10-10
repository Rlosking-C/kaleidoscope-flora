package com.rlosking.flora;

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

/**
 * All event-driven drink behaviours of Kaleidoscope Flora.
 *
 * <p>The drinks apply "marker" MobEffects (registered in {@link ModEffects}
 * without any logic); these handlers look for those markers and implement the
 * actual behaviour. Splitting it this way keeps every mechanic as plain
 * vanilla-or-NeoForge event code with zero custom packets or mixins.</p>
 *
 * <p><b>1.21.1 damage pipeline note:</b> NeoForge replaced the old Forge
 * damage events. {@code LivingIncomingDamageEvent} is the earliest, cancelable
 * stage (where the petal veil shatters projectiles and the kiss cloud
 * releases), {@code LivingDamageEvent.Pre} is the final stage before health is
 * applied (where reactions and the echo blink run), and amounts are read /
 * written through {@code getNewDamage()}/{@code setNewDamage(float)}.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraEvents {

    /** Level clock: a full day is 24000 ticks and night starts at 12000. */
    private static final long DAY_TICKS = 24000L;
    private static final long NIGHT_START = 12000L;

    /** Lily-of-the-valley poison cloud cooldown per player (5 seconds). */
    private static final Map<UUID, Long> KISS_COOLDOWN = new HashMap<>();

    /**
     * Chorus echo grace: game time (inclusive) until which a saved player
     * cannot drop below one heart. Entry exists only during the 5-second
     * window after an echo rescue.
     */
    private static final Map<UUID, Long> ECHO_GRACE_UNTIL = new HashMap<>();

    /**
     * Last known server-side XZ per player. Player movement is client
     * authoritative: the server receives position packets (absMoveTo) that
     * never run {@code Entity#move}, so both {@code deltaMovement} and
     * {@code walkDist} stay at zero horizontally on the server. Effects that
     * need "is this player walking" on the server read the real per-tick
     * step measured from actual positions instead.
     */
    private static final Map<UUID, double[]> LAST_XZ = new HashMap<>();

    /** Per-player horizontal movement vector (dx, dz) of the last server tick. */
    private static final Map<UUID, double[]> MOVE_DELTA = new HashMap<>();

    /**
     * The ground block each player's dig is currently unearthing (follows
     * the crosshair). A UUID present in this map means that player has an
     * active sniffer-soul brush-dig session.
     */
    private static final Map<UUID, BlockPos> BRUSH_TARGET = new HashMap<>();

    /**
     * Autumn Serenade: harvests left before the effect burns out, per player.
     *
     * <p>v0.3.4 turned this effect from time-based into <b>charge-based</b>.
     * Before, the drink ran for a fixed duration and every harvest inside it
     * rolled an independent multiplier; the only limit was the clock. Now the
     * effect carries a budget of {@link #HARVEST_CHARGES} harvests and expires
     * the moment the budget is spent, so the value of the cup is decided by how
     * the player spends it rather than by how fast they can run between fields.</p>
     *
     * <p>The entry is created lazily on the first harvest rather than at drink
     * time: the drink goes through Cookery's {@code TeacupItem}, which applies
     * the vanilla effect instance without calling back into this class, so there
     * is no reliable "just drank it" hook here. Lazy init is also self-healing -
     * a player who obtained the effect some other way (command, creative) still
     * gets the full budget.</p>
     */
    private static final Map<UUID, Integer> HARVEST_CHARGES_LEFT = new HashMap<>();

    /** Harvests an Autumn Serenade cup pays for before it expires (v0.3.4). */
    private static final int HARVEST_CHARGES = 20;

    /**
     * Absolution: harmful effects still refused, per player. Created lazily on
     * the first refusal for the same reason as the harvest budget above - the
     * drink applies a vanilla effect instance and never calls back into this
     * class, so there is no "just drank it" hook to seed the map from.
     */
    private static final Map<UUID, Integer> ABSOLVE_CHARGES_LEFT = new HashMap<>();

    /** Harmful effects an Absolution cup refuses before it expires (v0.3.4). */
    private static final int ABSOLVE_CHARGES = 3;

    // ==================================================================
    // Dew Flower Cake: the landing shockwave (four tiers)
    // ==================================================================

    /**
     * The Dew Flower Cake's landing shockwave: a four-step curve keyed on how
     * far the player actually fell.
     *
     * <p><b>The curve</b> (design section 1.4). Two blocks is the floor - below
     * that a plain jump is not an attack; twenty is the ceiling, so falling off
     * a mountain is not strictly better than falling off a tower.</p>
     *
     * <table>
     *   <caption>Impact tiers</caption>
     *   <tr><th>Drop</th><th>Radius</th><th>Knockback</th><th>Damage</th></tr>
     *   <tr><td>2-4</td><td>1.5</td><td>0.4</td><td>2</td></tr>
     *   <tr><td>5-9</td><td>2.5</td><td>0.8</td><td>4</td></tr>
     *   <tr><td>10-19</td><td>3.5</td><td>1.4</td><td>6</td></tr>
     *   <tr><td>20+</td><td>5.0</td><td>2.0</td><td>10</td></tr>
     * </table>
     *
     * <p><b>Minimum drop before it counts, and why that is not a bug.</b>
     * Because the height is measured from the apex of the airborne stretch,
     * ordinary flat-ground hopping never accumulates two blocks and so never
     * fires. That is the design's own answer to "can a player farm this by
     * bunny-hopping" (section 1.4's note) - no extra condition is needed.</p>
     */
    static final class DewImpact {

        /** Indexed by tier; [minDrop, radius, knockback, damage]. */
        private static final double[][] TIERS = {
                {2.0, 1.5, 0.4, 2.0},
                {5.0, 2.5, 0.8, 4.0},
                {10.0, 3.5, 1.4, 6.0},
                {20.0, 5.0, 2.0, 10.0},
        };

        /**
         * Surfaces that already break a fall: powder snow, hay, beds and honey
         * cushion the landing themselves, so stacking a shockwave on them would
         * double-count the block's whole purpose.
         */
        private static final Set<Block> SOFT_LANDINGS = Set.of(
                Blocks.POWDER_SNOW, Blocks.HAY_BLOCK, Blocks.HONEY_BLOCK,
                Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED,
                Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED,
                Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED,
                Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED);

        /** Radius of the particle burst, independent of the damage tier. */
        private static final double PARTICLE_RADIUS = 6.0;

        /**
         * Fall needed when a <em>deliberate</em> mid-air jump was used.
         *
         * <p>Below the design's two blocks, because a second jump taken while
         * already falling lands the whole arc lower - and above a plain jump's
         * 1.25 so that a single jump can never reach it. See {@link #settle}.</p>
         */
        private static final double MIDAIR_JUMP_MIN_DROP = 1.6;

        private DewImpact() {
        }

        /**
         * Settles one landing. Returns true when a shockwave actually fired, so
         * the caller can arm the repeat guard - returns false for every
         * non-qualifying landing, which must stay retryable.
         *
         * <p><b>A mashed jump disqualifies the landing outright.</b> That is the
         * rule that stops bunny-hopping from being an attack. It has to be an
         * outright refusal rather than a higher threshold, because the mashed hop
         * and the deliberate jump are only about half a block apart in height (a
         * hop mashed on the earliest possible tick peaks near 2.0, a deliberate
         * one near 2.5, a plain single jump at 1.25) and their ranges overlap the
         * design's own two-block floor from both sides. The client measures what
         * the server cannot - whether the player was still shooting upward when
         * they pressed - so the server does not have to guess. See
         * {@link AirStretch} and {@link FloraNetwork}.</p>
         *
         * <p><b>Two ways to qualify otherwise.</b></p>
         * <ul>
         *   <li><b>Fell two blocks or more</b> - the design's "from a height"
         *       rule, unchanged. This is what makes cliffs, towers and ordinary
         *       falls work.</li>
         *   <li><b>Jumped again deliberately and came down at least 1.6</b> -
         *       covers a second jump taken once the first was mostly spent, which
         *       is the case the design's two-block floor used to miss.</li>
         * </ul>
         *
         * <p>The tier is clamped to at least the first one, because the mid-air
         * route can legitimately fire below two blocks and the lookup would
         * otherwise index {@code TIERS[-1]}.</p>
         *
         * @param player     the falling player
         * @param drop       apex minus landing Y, in blocks
         * @param midAirJump whether the client reported a second jump
         * @param spamJump   whether that jump was taken while still rising
         */
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

            // The player is excluded from the sweep rather than filtered later,
            // which is what makes "the caster never takes their own shockwave"
            // true by construction instead of by a remembered condition.
            List<LivingEntity> hit = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(radius), other -> other != player);
            for (LivingEntity victim : hit) {
                victim.hurt(player.damageSources().playerAttack(player), (float) damage);
                // Horizontal, outward, with 0.6 of that upward - the design's
                // exact ratio, so targets are lifted off the ground without
                // being launched.
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
            // Achievements are earned by connecting, not by landing: an empty
            // shockwave is not a hit.
            if (!hit.isEmpty() && player instanceof ServerPlayer serverPlayer) {
                FloraAdvancements.dewImpact(serverPlayer, drop);
            }
            return !hit.isEmpty();
        }

        /**
         * Whether the landing surface counts: solid ground, not a fluid, and
         * not one of the self-cushioning blocks.
         *
         * <p>Fluids are checked because "landing in water raises no shockwave"
         * is explicit in the design - and because a player who just got the
         * effect from a cake is exactly the player likely to be aiming at
         * water.</p>
         *
         * <p><b>Both the fluid and the floor of it have to be excluded.</b> The
         * block check below only rejects the fluid's <em>surface</em>; a player
         * who sinks and stands on the bottom of a pond is on sand, so that check
         * passes and the attack fired. Reported in testing: "touching the ground
         * underwater also causes the fall attack". {@code isInFluidType} (any
         * fluid, including modded ones) plus powder snow is what makes "landing
         * in water" mean the whole body of water.</p>
         */
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


    // The pin's duration now lives in ModEffects.PIN_TICKS: the effect that
    // draws the pin's ring needs to know how far through the pin it is, so the
    // number has to be shared rather than owned here.

    /**
     * Slow amplifier for the pin. 6 is Slow VII, which floors ground speed -
     * the same value the Gaze effect uses for its freeze, so "held in place"
     * reads identically whether it came from a stare or a cake.
     */
    private static final int PIN_SLOW_AMPLIFIER = 6;

    /** Weakness amplifier for the pin: -4 attack damage per level. */
    private static final int PIN_WEAKNESS_AMPLIFIER = 4;

    /**
     * Player movement state while a Dew Flower Cake is up: the current airborne
     * stretch (its peak, and whether a second jump happened in it), plus the
     * game time of the last settled landing.
     *
     * <p>Tracks the peak of the <em>current airborne stretch</em>, not the
     * launch point - the design is explicit that the height difference is
     * "highest point of the jump minus the landing point", which is what makes
     * a normal jump and a fall off a cliff different amounts of damage.</p>
     */
    private static final Map<UUID, AirStretch> DEW_AIR = new HashMap<>();

    /**
     * Players whose client has told us about a mid-air jump, cleared at their
     * next landing.
     *
     * <p>The value is the part that matters: <b>true means the jump was taken
     * while the player was still shooting upward</b>, i.e. the key was being
     * hammered rather than a second jump being meant. Those hops must not
     * produce a shockwave, and - crucially - height cannot make that call. See
     * {@link FloraNetwork} for the measurements.</p>
     */
    private static final Map<UUID, Boolean> MIDAIR_JUMP_REPORTED = new HashMap<>();

    /**
     * Called from the payload handler when a client reports spending a jump.
     *
     * @param stillRising whether the player was still rising when they spent it
     */
    public static void noteMidAirJump(ServerPlayer player, boolean stillRising) {
        // A deliberate report outranks a mashed one: if the player both mashed
        // and then timed a real second jump in the same airborne stretch, the
        // real one is the one they meant.
        MIDAIR_JUMP_REPORTED.merge(player.getUUID(), stillRising,
                (existing, incoming) -> existing && incoming);
    }

    // REMOVED (2026-09-29): a 10-tick "same-landing guard" used to live here and
    // suppressed any landing within half a second of a previous shockwave. It was
    // redundant and it caused false negatives.
    //
    // Redundant: the airborne stretch is consumed at the landing
    // (`DEW_AIR.remove`), so the next grounded tick has no stretch and returns
    // before it could ever settle the same impact twice. There was nothing for a
    // guard to protect.
    //
    // False negatives: it skipped the whole settle, not just a repeat, so any
    // legitimate second landing inside half a second was silently swallowed.
    // Reported as "sometimes the double jump does not trigger the landing
    // damage" - and a hop cycle is right around that length, which is why it was
    // intermittent.

    /** Drops per Autumn Serenade harvest: uniformly 2x or 3x (v0.3.4). */
    private static final int HARVEST_MIN_MULTIPLIER = 2;
    private static final int HARVEST_MAX_MULTIPLIER = 3;

    /** Petal veil costs 10 seconds of duration per blocked projectile. */
    private static final int PETAL_VEIL_BLOCK_COST_TICKS = 200;

    /**
     * Petal veil internal cooldown: game time (exclusive) before which further
     * blocks are free. One volley costs one charge, not one per projectile.
     */
    private static final Map<UUID, Long> PETAL_VEIL_COOLDOWN_UNTIL = new HashMap<>();

    /** Length of the petal veil's repeated-block window (0.5 s, v0.3.4). */
    private static final int PETAL_VEIL_COOLDOWN_TICKS = 10;

    /** Lily poison cloud cooldown in ticks (5 seconds). */
    private static final int KISS_COOLDOWN_TICKS = 100;

    /**
     * Brushing a relic out of the ground takes this many ticks of holding
     * (2.4 s). Applied as the server-side use duration in
     * {@link #onBrushUseStart}; the use-item flag sync carries the early
     * end of the use to the client's brush animation.
     */
    private static final int BRUSH_DIG_TICKS = 48;

    /** Length of the one-heart echo grace window (12 seconds). */
    private static final int ECHO_GRACE_TICKS = 240;

    /**
     * Lang keys of the three instant marker effects. Vanilla potion tooltips
     * only print a duration when it exceeds 20 ticks, so these would show up
     * with no time at all - the tooltip handler labels them "(Instant)"
     * instead.
     */
    private static final Set<String> INSTANT_EFFECT_KEYS = Set.of(
            "effect.kaleidoscope_flora.purge",
            "effect.kaleidoscope_flora.fadeaway",
            "effect.kaleidoscope_flora.wish");

    /**
     * Ancient seeds shipped by Immortalers Delight (千古乐事), dug up by the
     * sniffer soul. Soft dependency: resolved lazily so this mod runs with
     * or without that mod installed.
     */
    private static final ResourceLocation[] ANCIENT_SEED_IDS = {
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "alfalfa_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "gelpitaya_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "himekaido_seed"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "kwat_wheat_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "sextlotus_seeds"),
            ResourceLocation.fromNamespaceAndPath("immortalers_delight", "warped_laurel_seeds")};

    /**
     * The pale moss block only exists in 1.21.1 through VanillaBackport, so it
     * is resolved lazily (backport blocks register after this class loads);
     * resolves to air, a harmless never-matching block, when no backport is
     * installed.
     */
    private static Block paleMossBlock;

    /** Cookery's warmth effect holder, resolved lazily on first use. */
    private static Holder<MobEffect> warmthEffect;

    private FloraEvents() {
    }

    // The stockpot route was deleted on 2026-09-22 (author decision): flower
    // drinks are brewed in the teapot and nowhere else. Three pieces of
    // support code went with the 26 stockpot recipes - the honey-bottle
    // container work-around, the teapot scoop-out, and the reflection that
    // drained a finished pot - because all three existed only to serve
    // drinks brewed in a stockpot. See the v0.4.0 design doc, section 8.

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // Chorus flower "Echo of the End": a fatal blow never lands. The echo
        // blinks you to a standable block nearby, grants 12 seconds of Fire
        // Resistance, Haste V and Jump Boost V for the escape, and holds you
        // at exactly one heart for 12 seconds (ECHO_GRACE window). The drink
        // pays for the rescue with itself - one cheat of death per cup.
        if (target.hasEffect(ModEffects.ECHO)
                && event.getAmount() >= target.getHealth() + target.getAbsorptionAmount()
                && target.level() instanceof ServerLevel server) {
            event.setCanceled(true);
            performEchoRescue(server, target);
            return;
        }

        // Lilac "Spring Waltz" (Cookery warmth): fully immune to freezing
        // damage - powder snow and freeze ticks included.
        if (event.getSource().is(DamageTypeTags.IS_FREEZING) && hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        // The same warmth at the other extreme: no fire, lava or hot floor
        // can scald the drinker either - the kettle sits above the flame,
        // never in it.
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && hasWarmth(target)) {
            event.setCanceled(true);
            return;
        }

        // Pink petals "Hanami Tale": every projectile that would hit the
        // drinker is shattered into petals instead. Each block spends 10
        // seconds of the veil's duration - beauty consumed to protect you.
        // Melee is unaffected.
        //
        // v0.3.4 adds a short internal cooldown, because a single volley of
        // projectiles arrives as many separate damage events in the same tick
        // (or in a burst of consecutive ticks). Without the gate, one skeleton
        // salvo would drain several 10-second charges in a fraction of a second
        // and the veil would read as "gone instantly" rather than "spent". The
        // cost per block is deliberately unchanged - only rapid repeats are
        // folded into one charge.
        if (target.hasEffect(ModEffects.PETAL_VEIL)
                && event.getSource().getDirectEntity() instanceof Projectile projectile) {
            long now = target.level().getGameTime();
            Long nextAllowed = PETAL_VEIL_COOLDOWN_UNTIL.get(target.getUUID());
            if (nextAllowed != null && now < nextAllowed) {
                // Still inside the window: the projectile is still shattered
                // (the veil is not bypassed), it just does not cost a charge.
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

        // Lily of the valley "May Kiss": being hurt releases a poison cloud
        // around the drinker (5 second cooldown).
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

    // ==================================================================
    // Final damage stage: vampirism, ignition, thorns, echo blink
    // ==================================================================

    /** One shared hook for everything that reacts to real damage being dealt. */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        float damage = event.getNewDamage();
        if (damage <= 0) {
            return;
        }
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity a ? a : null;

        // Red tulip "Crimson Heartbeat": 20% of dealt damage returns as
        // health. No particles by design: the client HUD reacts to any
        // health gain with the vanilla white-bordered heart flash - the
        // same animation natural regeneration shows after eating - so the
        // lifesteal healing announces itself exactly the way vanilla food
        // does (nothing shows at full health, matching vanilla behaviour).
        if (attacker != null && attacker.hasEffect(ModEffects.VAMPIRIC)) {
            attacker.heal(damage * (float) (FloraConfig.vampiricHealPercent() / 100.0));
        }

        // Allium "Fire Waltz": the drinker's melee hits set targets ablaze.
        if (attacker != null && attacker.hasEffect(ModEffects.FIREBRAND)) {
            victim.igniteForSeconds(6);
        }

        // Rose bush "Tender Thorns": the thorn answers in kind - the attacker
        // takes back a share of the very blow it landed, rolled per hit:
        // 70% half, 25% full price, 5% double. Plus weakness either way.
        if (victim.hasEffect(ModEffects.THORNS) && attacker != null && attacker != victim) {
            float roll = victim.getRandom().nextFloat();
            float share = roll < 0.05f ? 2.0f : roll < 0.30f ? 1.0f : 0.5f;
            attacker.hurt(victim.damageSources().thorns(victim), damage * share);
            attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }

        // Chorus echo grace (set by performEchoRescue): for 12 seconds after
        // the rescue the saved one cannot drop below one heart. Damage is
        // clamped, not negated - it still hurts, it just cannot finish the
        // job while the escape window lasts.
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

    /**
     * The chorus echo rescue: consumes the effect, blinks the victim onto a
     * random standable block within sight range, sets them to exactly one
     * heart, opens the 12 second one-heart grace window, and hands them
     * Fire Resistance + Haste V + Jump Boost V for 12 seconds to get away
     * with.
     */
    private static void performEchoRescue(ServerLevel server, LivingEntity target) {
        target.removeEffect(ModEffects.ECHO);
        ECHO_GRACE_UNTIL.put(target.getUUID(), server.getGameTime() + ECHO_GRACE_TICKS);

        // "大难不死" (Cheated Death): the echo answered a killing blow.
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

        target.setHealth(Math.min(2.0f, target.getMaxHealth())); // exactly one heart
        target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ECHO_GRACE_TICKS, 0)); // no scalding escape
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ECHO_GRACE_TICKS, 4)); // Haste V
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, ECHO_GRACE_TICKS, 4));      // Jump V
        // Haste only quickens digging in vanilla - an escape needs legs too.
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ECHO_GRACE_TICKS, 4)); // Speed V
    }

    /**
     * A random standable spot within 12 blocks horizontally / 4-6 vertically:
     * solid top-face ground below, two free collision blocks above it, no
     * fluid. Returns null when nothing qualifying is found in 32 attempts.
     */
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

    // ==================================================================
    // Cornflower "The Prussian Leap": fall damage immunity
    // ==================================================================

    /**
     * Cancelling the whole fall (instead of a zero damage multiplier) keeps
     * the entity logic intact. Vanilla only plays the landing thump when
     * damage actually lands, so a feather-soft landing would be silent - the
     * sound is reproduced here exactly the way {@code playBlockFallSound}
     * does it.
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.FEATHERFALL)) {
            event.setCanceled(true);
            playSoftLanding(entity, event.getDistance());
            return;
        }
        // Dew Flower Cake, v0.3.4: no fall damage at all while it lasts. This
        // replaces the design's original "still take the damage but never drop
        // below half a heart", which the author changed after the sister mod's
        // own fall-damage mixin turned out to cancel the damage outright - see
        // the design notes, section 3.4.1.
        //
        // Cancelling rather than zeroing the multiplier, for the same reason the
        // featherfall branch does: a zeroed multiplier still runs the landing
        // path, and the design wants a clean landing. The shockwave is settled
        // separately in tickDewLanding, so it is deliberately NOT triggered from
        // here - this hook may fire for falls that never had an apex tracked.
        if (entity.hasEffect(ModEffects.DEW_CAKE)) {
            event.setCanceled(true);
        }
    }

    /**
     * Reproduces vanilla's landing thump for a cancelled fall.
     *
     * <p>Vanilla only plays the sound when damage actually lands, so every
     * cancellation above silences a fall that should still be heard.</p>
     */
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

    // ==================================================================
    // Per-tick effects: ambient orbits and the thaw
    // ==================================================================

    /**
     * Runs for every living entity, server side only:
     * <ul>
     *   <li>Lilac "Spring Waltz" (Cookery warmth): freezing ticks melt away
     *       every tick, so powder snow can neither slow nor frostbite the
     *       drinker (belt to the damage-type cancellation in
     *       {@link #onIncomingDamage}).</li>
     *   <li>Pink petals "Hanami Tale": two cherry petals orbit the drinker
     *       for as long as the veil lasts - the beauty is visible even when
     *       nothing is being blocked.</li>
     *   <li>Peony "Coronation" (luck): golden motes circle the lucky one.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel server)) {
            return;
        }
        if (hasWarmth(entity)) {
            // Spring Waltz: the thaw AND the flame-out - freezing ticks melt
            // away every tick, and any fire the drinker waded through is
            // snuffed, so even lava leaves nothing but the swim.
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
        // Peony "Coronation" (luck): golden dust that fades in and out.
        if (entity.hasEffect(MobEffects.LUCK)) {
            coronationMotes(server, entity);
        } else {
            LUCK_START.remove(entity.getUUID());
        }
        // Orange tulip "Autumn Serenade": loose drops drift to the harvester.
        if (entity.hasEffect(ModEffects.HARVEST)) {
            attractDrops(server, entity);
        }
    }

    /** Two particles circling the entity at shoulder height, rotating over time. */
    private static void orbitParticles(ServerLevel server, LivingEntity entity, ParticleOptions particle) {
        long time = server.getGameTime();
        for (int i = 0; i < 2; i++) {
            double[] pos = orbitPos(entity, time, i);
            server.sendParticles(particle, pos[0], pos[1], pos[2], 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Shared orbit geometry of the circling effect particles: a 0.9-block
     * ring at shoulder height whose y bobs gently over time. Both orbit
     * users draw from this one formula, so their rings stay identical.
     */
    private static double[] orbitPos(LivingEntity entity, long time, int i) {
        double angle = time / 10.0 + i * Math.PI;
        return new double[]{
                entity.getX() + Math.cos(angle) * 0.9,
                entity.getY() + 1.0 + Math.sin(time / 12.0 + i * 2.0) * 0.2,
                entity.getZ() + Math.sin(angle) * 0.9};
    }

    /** When each lucky one's luck began, for the fade-in ramp. */
    private static final Map<UUID, Long> LUCK_START = new HashMap<>();

    /**
     * Golden motes for the coronation: a gold-dust orbit whose density ramps
     * up over the first two seconds and thins out over the last three, so
     * the blessing appears and dissolves instead of popping in and out.
     * Each dust mote also fades on its own over its lifetime, which makes
     * the orbit read as a soft golden haze rather than a hard ring.
     */
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

    /**
     * Autumn Serenade magnet: item entities within 5 blocks drift toward
     * the harvester - the falling leaves drawn to the basket. The pull grows
     * as the drop closes in, so items accelerate instead of hovering.
     */
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

    // ==================================================================
    // Blue orchid "First Bloom": food restores 50% more
    // ==================================================================

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();

        // Advancement hooks: every finished flora drink counts toward the
        // collection line and may fire one of the three drinking memes.
        if (entity instanceof ServerPlayer player && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.drinkFinished(player, event.getItem());
        }

        // Mid-Autumn: a 「安眠曲」Lullaby drunk at night brings one mooncake
        // out of the kitchen. It fires as the cup empties, so a whole pot
        // drunk through the night hands over one cake per cup.
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
            // v0.3.4 balance: +50% -> +20%. Half again on top of every meal was
            // strong enough to make the bloom the default way to eat rather
            // than a garnish on top of normal food.
            eater.getFoodData().eat(Math.round(food.nutrition() * 0.2f), food.saturation() * 0.2f);
        }
    }

    /**
     * True for the poppy's 「安眠曲」Lullaby specifically - the one drink that
     * calls a mooncake out at night.
     */
    private static boolean isLullaby(ItemStack stack) {
        return FloraAdvancements.isFloraDrink(stack)
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("lullaby");
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        // Hunger as it was before the first sip: by the time Finish fires,
        // the drink's food value has already restocked the bar, so "drank on
        // an empty stomach" can only be judged at the start.
        if (event.getEntity() instanceof ServerPlayer player
                && FloraAdvancements.isFloraDrink(event.getItem())) {
            FloraAdvancements.sipStarted(player);
        }
    }

    // ==================================================================
    // Pitcher plant "The Voracious Urn": kills count as +1 looting level
    // ==================================================================

    /**
     * The urn's greed has two mouths: every stack the player's kill dropped
     * has a 50% chance to grow by one (the binomial bonus vanilla Looting I
     * adds to common drops - NeoForge 1.21.1 has no looting-level event, so
     * the statistical outcome is reproduced instead), and then the whole loot
     * sometimes comes again: 70% of kills keep it at face value (x1), 30%
     * double it (x2), stacked on top of the looting bonus.
     */
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
        // The second mouth: duplicate the (already looting-grown) loot 30% of
        // the time; the other 70% the urn keeps its appetite to itself.
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
        // "贪心不足" (Greed Without End): three doubled drops in one urn.
        if (multiplier > 1 && player instanceof ServerPlayer urnBearer) {
            FloraAdvancements.urnDoubleDrop(urnBearer);
        }
    }

    // ==================================================================
    // Orange tulip "Autumn Serenade": mature crops drop x2-x3 for a budget of
    // twenty harvests, and farmland is not destroyed underfoot
    // ==================================================================

    /**
     * One harvest against the drink's budget.
     *
     * <p><b>Charge-based since v0.3.4.</b> Every mature crop broken while the
     * effect is up spends exactly one charge and rolls 2x or 3x independently -
     * so "twenty harvests of double or triple drops" is the whole promise, and
     * the effect removes itself when the twentieth is spent. The previous
     * time-based version let a fast player chain far more than twenty.</p>
     *
     * <p>The charge is spent <em>before</em> the drops are duplicated on
     * purpose: the last harvest in the budget still pays out. Decrementing
     * afterwards would need a special case for "this was the last one", and
     * an off-by-one here is exactly the kind of thing nobody notices until a
     * player complains the twentieth harvest gave nothing.</p>
     */
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
            // Defensive: the effect should already be gone by now. Belt and
            // braces so a stale entry can never grant unlimited drops.
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
        // One random multiplier of 2 or 3 per harvest; spawn the extra copies
        // of every drop entity.
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

    /**
     * The same drink's second clause: walking the fields does not ruin them.
     * Vanilla turns farmland back into dirt the moment something lands on it
     * hard enough, which an ordinary stride across a field clears easily - and
     * a harvest drinker crossing their own land is exactly who would do it.
     * NeoForge fires {@link BlockEvent.FarmlandTrampleEvent} for the attempt
     * and honours a cancellation, so the soil is left alone for as long as the
     * effect plays. Nothing else changes: drinkers without the effect, and
     * every mob, trample farmland exactly as they did.
     */
    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof LivingEntity walker && walker.hasEffect(ModEffects.HARVEST)) {
            event.setCanceled(true);
        }
    }

    /**
     * The walk's flowers are scenery, not a harvest: wildflowers that
     * {@link ModEffects.FlowerPathEffect} bloomed under an <b>empty</b> hand
     * give nothing back when broken (v0.3.4).
     *
     * <p>Before, the bloom was a free wildflower farm - walk a field, break the
     * trail, collect stacks. The block still appears and still grows to four
     * flowers if you linger; only the payout is gone, so the trail stays what it
     * looked like it was: decoration.</p>
     *
     * <p><b>Two ways a trail block can be destroyed, and only one of them has a
     * breaker.</b> A player breaking the flower directly arrives here with a
     * breaker, and that alone was the first implementation of this rule. Testing
     * found the other half: <em>break the dirt underneath</em> and the flower
     * above is removed by the engine's own neighbour update, which passes no
     * breaker at all - so the old filter missed it and the flower dropped. The
     * position is therefore the real test, and {@code isTrailBlock} is asked
     * first.</p>
     *
     * <p><b>Scoped so real wildflowers are untouched.</b> The obvious fix - ship
     * a loot table override for {@code minecraft:wildflowers} - would stop
     * <em>every</em> wildflower dropping, for every player, everywhere. Here,
     * only a position the free bloom actually placed is affected, and laying
     * flowers <em>with</em> a held carpet or petal stack is a deliberate
     * placement that keeps its drops.</p>
     *
     * <p>{@code BlockDropsEvent} is cancellable and carries a separate experience
     * field, so both halves of the payout are cleared - leaving the XP behind
     * would be a strange half-measure for a block that drops no item.</p>
     */
    @SubscribeEvent
    public static void onWalkFlowerDrops(BlockDropsEvent event) {
        BlockPos pos = event.getPos();
        boolean trail = ModEffects.FlowerPathEffect.isTrailBlock(event.getLevel(), pos);
        if (!trail) {
            // Not a free-bloom block: fall back to the player-based test, which
            // is what catches a trail block whose position was never recorded
            // (e.g. laid before the tracking existed in a running world).
            if (!(event.getBreaker() instanceof Player breaker)
                    || !breaker.hasEffect(ModEffects.FLOWER_PATH)
                    || ModEffects.FlowerPathEffect.isSheetItem(breaker.getMainHandItem())) {
                return;
            }
            Block held = event.getState().getBlock();
            if (held == Blocks.AIR || held != ModEffects.FlowerPathEffect.wildflowers()) {
                return;
            }
        }
        // Either path means the block is about to be gone: forget the position.
        ModEffects.FlowerPathEffect.untrackTrail(event.getLevel(), pos);
        event.setCanceled(true);
        event.setDroppedExperience(0);
    }

    /**
     * Forgets a dimension's trail when it unloads.
     *
     * <p>Without this the position set would outlive the world it describes, and
     * a later world reusing the same dimension could have its own wildflowers
     * silently stop dropping wherever the old coordinates happen to match.</p>
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel server) {
            ModEffects.FlowerPathEffect.clearTrail(server.dimension());
        }
    }

    // ==================================================================
    // White tulip "Absolution": harmful effects cannot land - for three of them
    // ==================================================================

    /**
     * {@code MobEffectEvent.Applicable} is fired before an effect is actually
     * applied, so vetoing here stops harmful effects from ever taking hold -
     * already-running ones are untouched on purpose (that cure is "When the
     * Wind Rises").
     *
     * <p><b>Charge-based since v0.3.4.</b> The drink used to buy a window of
     * time during which nothing harmful could land; now it buys
     * {@link #ABSOLVE_CHARGES} refusals and expires on the last one. The effect
     * therefore reads as "three cures" rather than "three minutes", which is
     * what the cup is actually worth to a player who drinks it before a fight
     * they know is coming.</p>
     *
     * <p>The charge is spent only when a refusal actually happens, so an
     * uneventful cup keeps its full budget - the player is never punished for
     * drinking early.</p>
     */
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

    // ==================================================================
    // Torchflower "Breath of the Ancients": brush-digging for relics
    // ==================================================================

    /**
     * While the sniffer soul is active, HOLDING a brush against diggable
     * ground (dirt family, mosses, mud, sand, red sand, gravel - but NEVER
     * vanilla suspicious sand/gravel, those stay pure vanilla archaeology)
     * digs for ancient relics - no sneaking required.
     *
     * <p><b>The dig rides the VANILLA brush-use pipeline end to end.</b>
     * {@code BrushItem#useOn} calls {@code startUsingItem} for any aimed
     * block; the resulting use state drives the first-person brush sweep,
     * the directional dust particles and the generic brush sounds, and the
     * server keeps every client's view of it in sync through the
     * LIVING_ENTITY_FLAGS entity data. This mod only hooks the NeoForge
     * {@link LivingEntityUseItemEvent} lifecycle:</p>
     *
     * <ul>
     * <li>{@code onBrushUseStart} - brush in the main hand + sniffer soul +
     * diggable ground under the crosshair (the same view-vector clip
     * {@code BrushItem} itself uses): open a session, play the sniffer
     * digging rumble right away, and shorten the vanilla 200-tick use to
     * {@link #BRUSH_DIG_TICKS}.</li>
     * <li>{@link #onBrushUseTick} - follow the crosshair; abandon the dig if
     * the soul runs out, the brush leaves the hand or the aim leaves
     * diggable ground.</li>
     * <li>{@link #onBrushUseFinish} - the use ran its course, a relic is
     * found: roll loot, spend brush durability, and pay for the find out
     * of the soul's remaining duration according to its value (the drink
     * starts with 5 minutes). Digs chain freely while the use key is held;
     * the soul's remaining duration is the only limiter.</li>
     * <li>{@link #onBrushUseStop} - the player let go early: cut the rumble
     * and play the sniffer's own digging-stop cue.</li>
     * </ul>
     *
     * <p>Because the server's use flag syncs to the client, ending the use
     * server-side (finish or guard failure) ends the client's brush
     * animation in the same beat - no wiggle left over after the item has
     * popped out. Loot table (weights sum to 120): torchflower seeds 30 /
     * pitcher pod 12 / coal 16 / iron nugget 14 / gold nugget 8 / bone 8 /
     * bone meal 12 / pottery sherd 10 - region-locked, see
     * {@link SherdPool} / Immortalers Delight ancient seed 8 / sniffer egg 2.</p>
     */
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
            return; // idle brushing or vanilla archaeology - not a dig
        }
        if (player.level().isClientSide()) {
            return; // client prediction: let vanilla set up its 200-tick use
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BRUSH_TARGET.put(player.getUUID(), blockHit.getBlockPos());
        // The sniffer's own digging rumble, right away - playing it only at
        // completion made the dig sound arrive after the item had already
        // popped out, which read as the sound lagging one dig behind.
        serverPlayer.serverLevel().playSound(null, blockHit.getBlockPos(), SoundEvents.SNIFFER_DIGGING,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        // Turn the vanilla 200-tick brush use into a 48-tick dig; the
        // use-flag sync carries this early end to the client animation.
        event.setDuration(BRUSH_DIG_TICKS);
    }

    /** The view-vector clip {@code BrushItem} uses for its own use ticks. */
    private static HitResult brushAim(Player player) {
        return ProjectileUtil.getHitResultOnViewVector(
                player, e -> !e.isSpectator() && e.isPickable(), player.blockInteractionRange());
    }

    /**
     * The player behind an active dig session in this use-item event, or
     * null when the use is client-side, not a brush, or has no session
     * (vanilla archaeology or idle brushing).
     */
    private static ServerPlayer digSessionPlayer(LivingEntityUseItemEvent event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getItem().is(Items.BRUSH)
                || !BRUSH_TARGET.containsKey(player.getUUID())) {
            return null;
        }
        return player;
    }

    /**
     * Keeps an active dig session pointed at whatever diggable ground the
     * crosshair sweeps over (one clip per tick, smoother than the old
     * every-4-ticks packet renewal). Any guard failing ends the dig the
     * same beat: the rumble is cut, the sniffer's digging-stop cue plays,
     * and cancelling this event makes vanilla's own use pipeline finish
     * the use - which stops the client animation through the flag sync.
     */
    @SubscribeEvent
    public static void onBrushUseTick(LivingEntityUseItemEvent.Tick event) {
        ServerPlayer player = digSessionPlayer(event);
        if (player == null) {
            return; // vanilla archaeology or idle brushing - not a dig
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

    /**
     * The use duration ran out: the dig succeeded. {@code Finish} also
     * fires for vanilla's full 200-tick idle brushing and archaeology, so
     * the session map is the marker of a real dig.
     */
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

    /**
     * The player let go of the use key mid-dig (or vanilla released the
     * use, e.g. the aim left every block). Cut the rumble and play the
     * sniffer's own "dig ended" cue.
     */
    @SubscribeEvent
    public static void onBrushUseStop(LivingEntityUseItemEvent.Stop event) {
        ServerPlayer player = digSessionPlayer(event);
        if (player != null) {
            clearBrushSession(player, false);
        }
    }

    /**
     * Measures each player's real per-tick horizontal step on the server
     * (see {@link #LAST_XZ}). Must run every tick with no early return:
     * effects like Gaze and Harvest read the step later via
     * {@link #lastMove}, because {@code Entity#move} deltas and
     * {@code walkDist} stay zero for players on the server.
     */
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

    /*
     * The Toasted Flower Cake's doubled hunger drain used to be ticked from
     * here (one exhaustion point per tick). It now lives in
     * mixin.ToastedHungerMixin, which doubles the exhaustion vanilla is already
     * causing - see that class for why the tick version was both far too fast
     * and wrong while standing still.
     */

    /**
     * Dew Flower Cake: remembers how high the player has been since last
     * touching the ground - and whether they jumped again in mid-air - then
     * settles a shockwave on the way down.
     *
     * <p>See {@link DewImpact} for the curve and the landing conditions, and
     * {@link AirStretch} for how the mid-air jump is recognised.</p>
     */
    private static void tickDewLanding(Player player, UUID uuid) {
        if (!player.hasEffect(ModEffects.DEW_CAKE)) {
            // The effect lapsed mid-air: forget the run rather than letting a
            // stale apex fire the next time the player happens to land.
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
            // The client's own report. The value says whether the jump was taken
            // while still rising, i.e. whether it was mashed - height cannot tell
            // the two apart, which is the whole reason the packet exists.
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
            // Landed without an airborne stretch tracked (effect applied on the
            // ground): nothing fell, nothing to settle.
            return;
        }
        double drop = stretch.apex - y;
        boolean solid = DewImpact.landsOnSolidGround(player);
        boolean fired = DewImpact.settle(player, drop, stretch.midAirJump, stretch.spamJump);
    }

    /**
     * One airborne stretch under a Dew Flower Cake: the peak reached, and what
     * the client said about any mid-air jump taken in it.
     *
     * <p><b>Height alone provably cannot decide this, and that is now settled by
     * measurement rather than assumed.</b> {@code LivingEntity#jumpFromGround}
     * sets the vertical velocity to the jump power <b>unconditionally</b>, so a
     * second jump does not add less for being early - it restores the full
     * impulse from wherever the player had reached. Working the arc out:</p>
     *
     * <ul>
     *   <li>a plain single jump peaks at about <b>1.25</b> blocks;</li>
     *   <li>a hop with the key mashed, spent on the earliest tick the mixin
     *       allows, peaks at about <b>2.0</b>;</li>
     *   <li>a deliberate second jump taken at the top peaks at about
     *       <b>2.5</b>.</li>
     * </ul>
     *
     * <p>So the two cases the design cares about sit less than half a block
     * apart, straddling the design's own two-block floor. Two earlier server-side
     * guesses were tried and each failed at one end of that range - per-tick rise
     * acceleration (false-fired on a dropped movement packet, so single jumps
     * fired) and peak-versus-single-jump (cannot see a late jump, and fires on an
     * early one because an early one clears the bar too). Both are removed.</p>
     *
     * <p>What is left is the client's report, which carries the one thing the
     * server cannot reconstruct: <b>was the player still shooting upward when
     * they pressed?</b> {@link #spamJump} is that, and {@link DewImpact#settle}
     * refuses the shockwave when it is set.</p>
     */
    private static final class AirStretch {

        /** Where the stretch began. */
        final double takeoffY;
        double apex;

        /** The client reported a mid-air jump in this stretch. */
        boolean midAirJump;

        /**
         * ... and that jump was taken while still rising, i.e. the key was being
         * hammered rather than a second jump being meant.
         */
        boolean spamJump;

        AirStretch(double y) {
            this.takeoffY = y;
            this.apex = y;
        }

        /** One airborne tick: all this has to do now is track the peak. */
        void observe(double y) {
            if (y > apex) {
                apex = y;
            }
        }
    }

    /**
     * Applies the one-shot tail of an effect when it runs out.
     *
     * <p>Only the Toasted Flower Cake needs it: the design's "乏力" is a Slow
     * applied <em>after</em> the buff, so it cannot live in the same effect
     * instance. {@code MobEffectEvent.Expired} fires when the duration reaches
     * zero.</p>
     *
     * <p>Deliberately not fired for {@code /effect clear} - NeoForge routes
     * that through {@code MobEffectEvent.Remove} instead, and the design already
     * accepts that a manually cleared effect skips its comedown. Reviving the
     * penalty there would punish the wrong action, and cancelling the removal
     * is not something this mod should ever do.</p>
     */
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

    /**
     * Raw Flower Cake hit: pins whatever a thrown snowball strikes.
     *
     * <p><b>The projectile is a plain vanilla snowball</b>, so there is nothing
     * on the entity itself to hook; the raw cake item only decides what gets
     * thrown. That makes this the one place the pin can be applied, and it also
     * means the behaviour rides an event NeoForge already fires for vanilla
     * projectiles - no entity type of our own, no mixin on {@code Snowball}.</p>
     *
     * <p><b>Trade-off, stated plainly:</b> the filter is "a snowball hit a
     * living entity". A snowball thrown by a dispenser or a command therefore
     * pins too. Distinguishing them would mean tagging the projectile at launch,
     * which needs either a custom entity (a permanent cost) or a per-entity map
     * that has to be cleaned up when a snowball misses and despawns. Neither is
     * worth it for a cake that a player has to be holding.</p>
     *
     * <p>{@code cancel()} stops the snowball's own impact handling - but the
     * projectile has to be discarded by hand, because a cancelled impact means
     * vanilla never reaches its own {@code discard()}. Skipping that would leave
     * the snowball hanging in the air.</p>
     */
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
        // One second, and all three meaning of "held": the slow stops the
        // walking, the weakness stops the hitting, and the marker is what
        // mixin.PinJumpMixin reads to refuse the jump. The marker also draws the
        // ring of petals for as long as it lasts - see ModEffects.PinnedEffect.
        //
        // The MARKER goes in through forceAddEffect; the two debuffs go through
        // the ordinary path. The Wither and the Ender Dragon both override
        // addEffect to return false unconditionally - every effect is refused -
        // so a plain addEffect left the design's "works on bosses too" silently
        // unachieved: the cake vanished with its chime and the boss kept walking.
        // Force is the right tool for the marker, because the marker *is* the pin
        // (it drives both the freeze in PinHoldMixin and the ring). The debuffs
        // stay on the normal path deliberately: they are cosmetic next to a
        // frozen target, and overriding a boss's blanket immunity for them would
        // be a wider deviation than the design asks for.
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

    /**
     * The living thing a thrown raw flower cake just hit, or null if it hit
     * something that cannot be pinned.
     *
     * <p><b>Unwrapping the hit matters for exactly one mob, and it is a boss.</b>
     * A projectile that strikes the Ender Dragon hits one of its {@link PartEntity}
     * hitboxes rather than the dragon entity, and {@code EnderDragonPart} is not a
     * {@code LivingEntity} - so the old
     * {@code hit.getEntity() instanceof LivingEntity} test failed and the whole
     * handler returned before it even played the impact sound. The dragon is the
     * only entity in the game hit this way; the other multipart mobs that matter
     * here (the ghast, the wither) are plain {@code LivingEntity}s.</p>
     */
    private static LivingEntity pinTarget(Entity hit) {
        if (hit instanceof LivingEntity living) {
            return living;
        }
        if (hit instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent) {
            return parent;
        }
        return null;
    }

    /**
     * The instant of the pin: an outward burst of white sparks and a glassy
     * chime.
     *
     * <p>Design §1.6 asks for a ring of cold-white {@code CRIT} spreading out
     * from the target at the moment of the hit, and for the pin to announce
     * itself with a short chime on top of the snowball's own impact sound (which
     * vanilla plays and this does not replace).</p>
     *
     * <p>The burst is spawned as explicit points on a circle rather than as one
     * {@code sendParticles} call with a spread, because a spread box reads as a
     * puff - the ring is the part that says "held", and it needs the points to
     * actually be on a circle. The vertical spread is deliberately tiny so it
     * stays a ring seen from the side, not a sphere.</p>
     *
     * <p>The radius and the point count come from {@code ModEffects}' pin-ring
     * helpers - the same ones the pin's own tick uses. The burst has to size
     * itself to the target for the same reason the ring does, and the two must
     * agree, or the burst would visibly change size as the persistent ring takes
     * over from it.</p>
     */
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
        // A chime, pitched up so it reads as ice rather than as a bell, and at a
        // volume that sits under the snowball hit instead of covering it.
        server.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.6f);
    }

    /**
     * Discards every trace of a player's brush-dig session - and stops the
     * digging rumble. {@link SoundEvents#SNIFFER_DIGGING} is a 6.7 s vanilla
     * one-shot (the vanilla sniffer digs for 120 ticks, matching the file),
     * but this mod's dig is only {@link #BRUSH_DIG_TICKS} = 48 ticks, so
     * without an explicit stop the rumble kept playing for another ~4 s
     * after the session was already gone. A {@link ClientboundStopSoundPacket}
     * (the /stopsound packet) cuts it on every client in the same level; an
     * interrupted dig additionally gets the sniffer's own "dig ended" cue
     * ({@code SNIFFER_DIGGING_STOP}), mirroring the vanilla RISING state.
     */
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

    // ==================================================================
    // Pink tulip "Rosy Stride": land-style view bob while on the water
    // ==================================================================

    /**
     * The petal film holds the drinker's feet ABOVE the fluid, so
     * {@code Entity#move()} reports no block collision and
     * {@code Player#aiStep}'s bob branch - which requires
     * {@code onGround() && !isSwimming()} - zeroes out. Walking on water
     * therefore had no view bob at all.
     *
     * <p>This runs in {@code PlayerTickEvent.Post}, i.e. AFTER that update,
     * and re-applies the exact vanilla formula
     * ({@code bob = oBob + (min(0.1, horizSpeed) - oBob) * 0.4}) - {@code oBob}
     * was already stored correctly by {@code aiStep}, so the camera sways
     * on the film precisely like it does on land: same amplitude, same
     * frequency, decaying when standing still or mid-jump.</p>
     *
     * <p>Must run on BOTH sides - the bob fields are only ever read for the
     * client player, but the event has no side filter anyway, and on land
     * the recomputation is a no-op (identical numbers) so nothing fights.</p>
     */
    @SubscribeEvent
    public static void onPlayerTickPetalBob(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.PETALWALK)
                || player.isSwimming()
                || player.isDeadOrDying()) {
            return; // diving deep or dead: no bob, like vanilla
        }
        if (player.onGround() || !isOnPetalFilm(player)) {
            return; // real ground handles it natively; mid-air/jumping decays like vanilla
        }
        float stride = Math.min(0.1F, (float) player.getDeltaMovement().horizontalDistance());
        player.bob = player.oBob + (stride - player.oBob) * 0.4F;
    }

    /**
     * True when the player's feet rest on the petal film riding a water
     * surface - the same window {@code PetalWalkEffect} pins them inside
     * (a hair above the surface, with the gravity-sag tolerance below it).
     * Package-private: {@link FloraAdvancements} reuses it for the water
     * crossing award.
     */
    static boolean isOnPetalFilm(Player player) {
        BlockPos support = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        FluidState fluid = player.level().getFluidState(support);
        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }
        // Same topmost-layer rule as PetalWalkEffect: no film (and no bob)
        // while inside the body of water, only on its surface.
        if (player.level().getFluidState(support.above()).is(FluidTags.WATER)) {
            return false;
        }
        double surface = support.getY() + fluid.getHeight(player.level(), support);
        double feet = player.getY();
        return feet >= surface - 0.05 && feet <= surface + 0.15;
    }

    // ==================================================================
    // Advancements: per-tick bookkeeping and the lullaby dawn
    // ==================================================================

    /**
     * Server-side advancement bookkeeping for every player, once per tick.
     * Delegates to {@link FloraAdvancements#tick}: clears meme counters of
     * lapsed effects, accumulates the Rosy Stride water crossing, and once
     * per second checks the hidden collector award.
     */
    @SubscribeEvent
    public static void onPlayerTickAdvancements(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        FloraAdvancements.tick(player);
        // v0.3.5: the guide book. Hands it out on the first join and flips it to a
        // flower's page the first time that flower is picked up. Both calls are
        // inert when Patchouli is absent - see PatchouliGuide for the rule.
        com.rlosking.flora.compat.PatchouliGuide.tickFlowerUnlock(player);
    }

    /**
     * "一晌贪欢" (A Moment of Stolen Joy): the lullaby drinker falls asleep
     * drowsy and wakes at dawn with the effect still running - the night
     * skip advances the clock but never the buff's duration, so the nap
     * really did cost the dreamer nothing. {@code PlayerWakeUpEvent} fires
     * on both logical sides; only the server awards. Waking at dawn means
     * the game time landed in the first ticks of a fresh day - a wake-up
     * in the middle of the night (interrupted by a monster, say) is
     * exactly the "stolen" joy this award refuses.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(ModEffects.DROWSY) && player.level().getDayTime() % 24000L < 2400L) {
            FloraAdvancements.award(player, FloraAdvancements.EVENT_LULLABY_DAWN);
        }
    }

    /**
     * Everything the sniffer soul can brush relics out of: the dirt family,
     * mosses, mud, sand, red sand and gravel. Vanilla suspicious sand and
     * suspicious gravel are deliberately excluded BEFORE the {@code #sand}
     * tag check - that vanilla tag DOES contain suspicious sand, and without
     * this guard the dig would hijack vanilla archaeology sites. Brushing a
     * suspicious block must resolve through the vanilla loot path, untouched.
     */
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

    /** One dig result: what you find and how much buff duration it costs. */
    private record SnifferDig(ItemStack stack, int costTicks) {
    }

    /**
     * Vanilla pottery sherd sets, mirroring where vanilla archaeology finds
     * them: desert pyramids and wells, warm and cold ocean ruins, trail
     * ruins. The brush dig borrows the same regional logic - the biome you
     * dig in decides which ancient culture left its sherds behind, so
     * collecting all twenty sherds means travelling, exactly like hunting
     * vanilla sites.
     */
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

    /**
     * Maps the dig position to its regional sherd set. Deserts and badlands
     * (hot, dry - no precipitation) hold the pyramid and well sherds; ocean
     * and beach ground holds ruin sherds, warm or cold by biome temperature;
     * snowy ground shares the cold ruin set; everything else gets the trail
     * ruins set. Non-overworld dimensions have no vanilla ruins, so they
     * default to the trail ruins set.
     */
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

    /**
     * Weighted ancient-relic table for the brush dig; costs are 1/20 of a
     * second. Pottery sherds are region-locked: the dig position picks one
     * of the {@link SherdPool} sets, then the roll picks a sherd from it.
     */
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
            // Graceful degradation: with Immortalers Delight absent the seed
            // resolves to air, so fall back to coal instead of charging the
            // player 25s of duration for nothing.
            ItemStack seeds = rollAncientSeed(random);
            return seeds.isEmpty()
                    ? new SnifferDig(new ItemStack(Items.COAL), 8 * 20)
                    : new SnifferDig(seeds, 25 * 20);
        }
        return new SnifferDig(new ItemStack(Items.SNIFFER_EGG), 60 * 20);
    }

    /** One of the six ancient seeds, or nothing when the mod is absent. */
    private static ItemStack rollAncientSeed(RandomSource random) {
        Item item = BuiltInRegistries.ITEM.get(ANCIENT_SEED_IDS[random.nextInt(ANCIENT_SEED_IDS.length)]);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ==================================================================
    // Drink tooltips: mechanical description and instant labels
    // ==================================================================

    /**
     * Cookery renders each drink's maxim (dark gray, italic) followed by the
     * vanilla-style effect list with durations. Two touches are added here:
     * <ul>
     *   <li>A one-line mechanical description right below the maxim, in plain
     *       gray - clearly a different voice than the italic quote.</li>
     *   <li>The three instant marker effects carry duration 1, and the
     *       vanilla potion tooltip prints no time for those - they get an
     *       "(instant)" label so every effect line notes how long it lasts.</li>
     * </ul>
     *
     * <p><b>Drinks, and the three flower cakes.</b> Everything else this mod
     * registers - the tea bags and the mooncake - is a plain item with no
     * {@code .desc} key, and a translatable component whose key is missing
     * renders as the raw key text. That is how the tea bags came to show
     * {@code tooltip.kaleidoscope_flora.<id>.desc} in their own tooltip. Hence the
     * gates: a namespace check alone is not enough to keep a newly added item out
     * of this handler, and an item must not be let in until its key exists.</p>
     *
     * <p>The cakes were added to the gate in v0.3.4 after the plain question "what
     * does Toasted do?" turned out to have no answer in game: their tooltips said
     * nothing and JEI carries only the drinks. Their {@code .desc} keys are short
     * mechanical lines in the same voice as the drinks', not flavour text - the
     * maxims and quotes remain the designer's.</p>
     */
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
        // Insert right below the maxim: Cookery ends the quote with a blank
        // line before the effect list, so the first blank line is the anchor.
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

        // Rename Cookery's creative-tab attribution for flora drinks: the
        // drinks stay in Cookery's "美食" tab, but the blue hint line should
        // speak for this mod. Cookery (the alphabetically earlier mod id)
        // adds its line before this handler sees it.
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

    // ==================================================================
    // Shared helpers
    // ==================================================================

    /**
     * Re-adds an effect with the given cost subtracted from its remaining
     * duration (durations are immutable on live instances). The effect
     * simply ends when the cost exceeds what is left.
     */
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

    /** Cookery's warmth effect holder, resolved once (Cookery is a hard dependency). */
    private static boolean hasWarmth(LivingEntity entity) {
        if (warmthEffect == null) {
            warmthEffect = BuiltInRegistries.MOB_EFFECT.getHolder(
                    ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "warmth")).orElse(null);
        }
        return warmthEffect != null && entity.hasEffect(warmthEffect);
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    /**
     * The player's horizontal movement vector (dx, dz) of the last server
     * tick. Player movement is client authoritative, so on the server
     * {@code getDeltaMovement()} and {@code walkDist} are always ~0 for
     * players - effects that need the real step read it from here. One tick
     * of latency is inherent (positions are diffed after the player tick).
     */
    static double[] lastMove(Player player) {
        double[] delta = MOVE_DELTA.get(player.getUUID());
        return delta == null ? new double[]{0.0, 0.0} : delta;
    }

    /** v0.3.5: hands the guide to a player the first time they join. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        com.rlosking.flora.compat.PatchouliGuide.giveOnce(event.getEntity());
    }

    /** Frees the per-entity cooldown maps when a player leaves. */
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
        // Both dew-cake maps are keyed by UUID and would otherwise outlive the
        // session; a stale "reported a mid-air jump" flag in particular would
        // arm the next login's first landing.
        DEW_AIR.remove(uuid);
        MIDAIR_JUMP_REPORTED.remove(uuid);
        ModEffects.FlowerPathEffect.clearPlayer(uuid);
        ModEffects.GazeEffect.clearPlayer(uuid);
        ModEffects.PetalWalkEffect.clearPlayer(uuid);
        FloraAdvancements.clearPlayer(uuid);
        clearBrushSession((ServerPlayer) event.getEntity(), false);
    }
}
