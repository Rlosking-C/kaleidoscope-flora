package com.rlosking.flora.mixin;

import com.rlosking.flora.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a pinned mob from jumping, which is the one third of "定身" that has no
 * other hook.
 *
 * <p><b>Why this is a mixin and not an event.</b> The Raw Flower Cake's pin is
 * specified as three things - no walking, no attacking, no jumping - and two of
 * them are free:</p>
 * <ul>
 *   <li><b>Walking</b> is {@code MOVEMENT_SLOWDOWN} VI, applied alongside the
 *       pin. Amplifier 6 is beyond anything vanilla produces, which is exactly
 *       what makes it read as "held in place" rather than "slowed".</li>
 *   <li><b>Attacking</b> is {@code WEAKNESS}, which subtracts flat damage from
 *       the {@code attack_damage} attribute - enough to floor a mob's melee.</li>
 *   <li><b>Jumping</b> has nothing. There is no vanilla "no jump" flag, and
 *       {@code MobEffects.JUMP} cannot be inverted: its contribution is
 *       {@code 0.1 * (amplifier + 1)}, so amplifier -1 is <em>identical to
 *       having no effect at all</em> - it takes away the bonus, not the
 *       jump. NeoForge's {@code LivingEvent.LivingJumpEvent} would be the
 *       natural place, but it is explicitly documented as not being
 *       cancellable, and it fires <em>after</em> the jump is already
 *       committed.</li>
 * </ul>
 *
 * <p>So this injects at the head of {@code Mob#aiStep} - the per-tick AI entry
 * point, which is where a mob's jump request comes from - and clears the
 * jumping flag before it is acted on. {@code setJumping(false)} rather than
 * cancelling the whole method: everything else the mob wanted to do this tick
 * (looking, targeting, pathing) still happens, so a pinned mob looks held
 * rather than frozen solid mid-thought.</p>
 *
 * <p>Only {@link Mob} is targeted. Players jump through a different code path
 * (input-driven, {@code LocalPlayer} client-side), and the pin is a thrown
 * projectile - a player who gets hit by their own cake is pinned by the
 * slow and the weakness, which is the more forgiving outcome anyway.</p>
 */
@Mixin(Mob.class)
public abstract class PinJumpMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void flora$forbidJumpWhilePinned(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModEffects.PINNED)) {
            self.setJumping(false);
        }
    }
}
