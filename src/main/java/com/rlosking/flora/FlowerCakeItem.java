package com.rlosking.flora;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A finished flower cake: eaten for a short, sharply-defined buff, and
 * sneak-placed as a block.
 *
 * <p>Both variants are the same shape - eat, get a {@link ModEffects} marker
 * plus whatever vanilla effects the route implies - so they share one class and
 * differ only in which branch {@link #finishUsingItem} takes.</p>
 *
 * <p><b>No hunger value is added here.</b> Food values come from the item
 * properties at registration ({@link FlowerCakes}); this class only adds the
 * effects on top, so the two can be tuned independently.</p>
 *
 * <p><b>Why this extends {@link PlaceableCakeItem} and not plain {@code Item}.</b>
 * The cake is also a block now (sneak + right-click sets one down). That base
 * class carries the sneak gate and the stacking; everything about eating stays
 * right here. Because it is a {@code BlockItem}, the language key is
 * {@code block.kaleidoscope_flora.<name>} rather than {@code item....} - see the
 * note in {@link FlowerCakes}.</p>
 */
public class FlowerCakeItem extends PlaceableCakeItem {

    /**
     * Toasted cake duration in ticks (40 s).
     *
     * <p>Hard-coded rather than routed through {@code FloraConfig}'s
     * {@code effectDurationMultiplier}: that multiplier is documented as "every
     * <em>drink's</em> effect duration" and is applied to the tea catalogue. A
     * cake is a different item line, and silently scaling it would make the
     * config mean something its own description does not say. If cakes should
     * scale too, that is a deliberate change to the config's contract.</p>
     */
    private static final int TOASTED_TICKS = 40 * 20;

    /** Dew cake duration in ticks (60 s). */
    private static final int DEW_TICKS = 60 * 20;

    public FlowerCakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * Applies the effect that belongs to this cake.
     *
     * <p><b>The identity is read BEFORE {@code super}, and that ordering is not
     * cosmetic.</b> {@code Item#finishUsingItem} delegates to
     * {@code LivingEntity#eat}, which ends with {@code food.consume(1, this)} -
     * it shrinks the very stack object it was handed. Eating the <b>last</b> cake
     * of a stack therefore leaves {@code stack} empty, and an empty
     * {@code ItemStack} answers {@code false} to every {@code is(...)} test. So
     * the original version of this method</p>
     *
     * <pre>{@code
     * ItemStack result = super.finishUsingItem(stack, level, entity);
     * if (stack.is(FlowerCakes.DEW.get())) { ... }   // false when that was the last one
     * }</pre>
     *
     * <p>silently did nothing for the final cake of every stack - no Toasted
     * buff, no double jump, and no config-driven jump count. Reported as "the
     * config option has no effect", which is exactly how it presents: the option
     * was fine, the effect it feeds was never applied.</p>
     *
     * <p>Reading the identity first fixes it and costs nothing: two
     * {@code is} calls against the stack as it was handed in.</p>
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean toasted = stack.is(FlowerCakes.TOASTED.get());
        boolean dew = stack.is(FlowerCakes.DEW.get());
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!(level instanceof ServerLevel) || !(entity instanceof Player eater)) {
            return result;
        }
        if (toasted) {
            eatToasted(eater);
        } else if (dew) {
            eatDew(eater);
        }
        return result;
    }

    /**
     * Toasted: speed and strength, a mining edge, then the price.
     *
     * <p><b>Changed 2026-09-29: Strength replaced the attack-speed role.</b> The
     * design used to ask for +20% attack speed, which had to be faked with Haste
     * because vanilla has no attack-speed-only effect - and that pulled +10%
     * mining along with it as an unavoidable side effect. The design now asks for
     * <b>Speed and Strength</b> instead, which are both vanilla effects with
     * their own icons, so the pair reads plainly in the effect bar and no custom
     * icon is needed for it.</p>
     *
     * <p><b>Haste stays, and now only for the mining efficiency</b> - which the
     * design explicitly asks to keep. It still brings its +10% attack speed with
     * it, because vanilla offers no mining-only effect; that is the same
     * trade-off the old version documented, just no longer the headline.
     * Do not "fix" the amplifier.</p>
     *
     * <p><b>The marker is invisible, on purpose.</b> {@link ModEffects#TOASTED_CAKE}
     * is a carrier, not a message: it exists so the doubled hunger drain and the
     * comedown can recognise "this player is under the cake" (see
     * {@code ToastedHungerMixin} and {@link FloraEvents#onCakeEffectExpired}).
     * The player-facing information is Speed, Strength and Haste, each of which
     * already has an icon - so the marker is applied with
     * {@code visible = false} and does not take a slot in the effect bar. That is
     * what "the buff texture is not needed" means in the design note.</p>
     *
     * <p>The doubled hunger drain and the sluggish comedown are NOT applied
     * here: they are per-tick / on-expiry behaviour and live in
     * {@link FloraEvents}.</p>
     */
    private void eatToasted(Player eater) {
        // 4th/5th args: ambient = false, visible = false - a carrier, not an icon.
        eater.addEffect(new MobEffectInstance(ModEffects.TOASTED_CAKE, TOASTED_TICKS, 0, false, false));
        // Strength I: +3 attack damage.
        eater.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, TOASTED_TICKS, 0));
        // Speed I is +20% movement, exactly what the design asks for.
        eater.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, TOASTED_TICKS, 0));
        // Haste I: kept for the +10% mining efficiency only (its +10% attack
        // speed is an unavoidable rider - see the note above).
        eater.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, TOASTED_TICKS, 0));
    }

    /**
     * Dew: the marker carries the extra-jump count, and the landing shockwave
     * hangs off the same effect.
     *
     * <p><b>The count comes from {@code FloraConfig.doubleJumpCount} and then
     * travels inside the effect as its amplifier</b> (amplifier = count - 1, so
     * amplifier 0 is one extra jump). That split matters: the config supplies
     * the default a player can tune, and the effect instance is what the jump
     * logic actually reads - so a future item that wants three jumps only has to
     * apply this effect with amplifier 2, and nothing in the jump code changes.
     * Tested complaint: "the dew cake still limits me to one extra jump and the
     * config screen has no jump-count option" - both halves of that came from
     * this value being hard-coded to 1 and never configured.</p>
     *
     * <p>A configured 0 applies amplifier 0 anyway rather than skipping the
     * effect: the effect also carries the shockwave and the fall immunity, so
     * skipping it would silently disable those too. 0 means "no extra jump",
     * not "no dew cake".</p>
     */
    private void eatDew(Player eater) {
        int extraJumps = Math.max(0, FloraConfig.doubleJumpCount());
        eater.addEffect(new MobEffectInstance(ModEffects.DEW_CAKE, DEW_TICKS, Math.max(0, extraJumps - 1)));
    }
}
