package com.rlosking.flora.mixin.client;

import com.rlosking.flora.FloraNetwork;
import com.rlosking.flora.KaleidoscopeFlora;
import com.rlosking.flora.ModEffects;
import com.rlosking.flora.client.DewJumpFeedback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Dew Flower Cake's extra mid-air jump, implemented here rather than
 * borrowed from the sister mod.
 *
 * <p><b>Why this is not "just call {@code kaleidoscope_world_liquor}'s effect".</b>
 * That was the original plan, and it was dropped for four reasons recorded in
 * the design notes (section 3.4.1). The two that shape this file:</p>
 * <ul>
 *   <li>the sister mod cancels <em>all</em> fall damage whenever its effect is
 *       up, decided inside its own mixin where this mod cannot reach it - so
 *       "does the dew cake stop you dying from a fall" would have been its
 *       decision, not ours;</li>
 *   <li>it is a soft dependency, so with the sister mod absent the jump would
 *       have vanished entirely.</li>
 * </ul>
 * <p>Doing it here keeps both under this mod's control, and needs one counter.</p>
 *
 * <p><b>Why the count is the amplifier plus one.</b> The design asks for the
 * number of extra jumps to live in the effect rather than in code, so that a
 * future triple-jump item is a data change and not a code change. Amplifier 0
 * must therefore mean "one extra jump" (a literal double jump), which makes the
 * formula {@code amplifier + 1} - the same contract the sister mod used, kept
 * deliberately so the two read alike.</p>
 *
 * <p><b>Client only, and that is correct.</b> Player movement is client
 * authoritative in Minecraft: the client simulates its own motion and the
 * server is told where the player ended up. So a jump that only exists on the
 * client still lands the player where the client says, and nothing on the server
 * needs to agree in advance.</p>
 *
 * <p><b>Ground tracking is a mixin-local field, not {@code onGround()}.</b> The
 * counter has to refill at the moment the player leaves the ground - not while
 * they are merely standing on it - because refilling every tick they stand would
 * be harmless but refilling while airborne would grant infinite jumps. So this
 * remembers the previous tick's ground state and refills on the falling edge.
 * {@code lastOnGround} on {@code LocalPlayer} is private, hence the local copy
 * rather than a shadow: reading it would couple this to a field name in a class
 * that has no obligation to keep it.</p>
 */
@Mixin(LocalPlayer.class)
public abstract class DoubleJumpLocalPlayerMixin {

    /** Extra jumps still available in this airborne stretch. */
    @Unique
    private int flora$jumpsLeft;

    /** Whether the player was on the ground at the end of the previous tick. */
    @Unique
    private boolean flora$wasOnGround = true;

    /**
     * Whether the jump key was held at the end of the previous tick.
     *
     * <p>Needed to detect the <b>press</b> rather than the <b>hold</b> - see
     * {@link #flora$doubleJump}. {@code Input#jumping} is a level, not an edge:
     * it stays true for as long as the key is down, which for a normal keypress
     * is several ticks.</p>
     */
    @Unique
    private boolean flora$jumpWasHeld;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void flora$doubleJump(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        boolean onGround = self.onGround();
        MobEffectInstance dew = self.getEffect(ModEffects.DEW_CAKE);

        // The key's rising edge, sampled before any early return so the held
        // flag cannot go stale.
        //
        // v0.3.4 fix: this used to test input.jumping directly, i.e. "is the key
        // down". A human keypress lasts far longer than one tick, and the guard
        // below deliberately skips the first airborne tick - so a single press
        // was still down on the tick after that and spent the extra jump by
        // itself. Reported in testing: "I press space once and it jumps twice,
        // triggering the double jump". Requiring an edge means the second jump
        // takes a second, deliberate press, which is what a double jump is.
        boolean jumpHeld = self.input.jumping;
        boolean jumpPressed = jumpHeld && !flora$jumpWasHeld;
        flora$jumpWasHeld = jumpHeld;

        if (onGround || self.onClimbable()) {
            // Refill on the ground. Without the climbable clause a player on a
            // ladder would spend their jumps and then be unable to refill
            // without touching down, which reads as a bug.
            int before = flora$jumpsLeft;
            flora$jumpsLeft = dew == null ? 0 : dew.getAmplifier() + 1;
            if (dew != null && flora$jumpsLeft != before) {
                // Diagnostics for the "it still lets me triple jump" report: the
                // amplifier is baked into the effect when the cake is EATEN, so a
                // config change alone leaves the effect in play carrying its old
                // count. One line per landing tells those two cases apart.
                KaleidoscopeFlora.LOGGER.info(
                        "[flora] dew jump refill: amplifier={} jumpsLeft={} onGround={} climbable={}",
                        dew.getAmplifier(), flora$jumpsLeft, onGround, self.onClimbable());
            }
            flora$wasOnGround = true;
            return;
        }
        boolean justLeftGround = flora$wasOnGround;
        flora$wasOnGround = false;
        if (dew == null) {
            // Effect lapsed mid-air: drop the budget so re-drinking in flight
            // cannot top up a jump the player already spent.
            flora$jumpsLeft = 0;
            return;
        }
        if (justLeftGround || flora$jumpsLeft <= 0) {
            // The vanilla jump itself is not ours to spend: the tick the player
            // leaves the ground they are already moving upward, so charging one
            // here would make the cake grant one fewer jump than it says.
            return;
        }
        if (jumpPressed) {
            flora$jumpsLeft--;
            KaleidoscopeFlora.LOGGER.info(
                    "[flora] dew air jump: amplifier={} left={}",
                    dew.getAmplifier(), flora$jumpsLeft);
            // Sample the velocity BEFORE the jump resets it: this is the only
            // record of whether the player was still shooting upward, which is
            // what tells a deliberate second jump from a mashed one. See
            // FloraNetwork - the two are inseparable by height, because
            // jumpFromGround sets the velocity unconditionally.
            boolean stillRising = self.getDeltaMovement().y > DewJumpFeedback.RISING_VELOCITY;
            self.jumpFromGround();
            DewJumpFeedback.play(self);
            FloraNetwork.sendMidAirJump(stillRising);
        }
    }

}
