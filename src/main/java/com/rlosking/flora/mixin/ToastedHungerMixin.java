package com.rlosking.flora.mixin;

import com.rlosking.flora.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Toasted Flower Cake: hunger drains twice as fast.
 *
 * <p><b>Doubling the exhaustion as it is caused, rather than adding a trickle
 * every tick.</b> The first implementation of this added one exhaustion point
 * per tick on the player tick event, which is 20 per second - far past "twice as
 * fast" and reported in testing as "hunger drains fast even when I am not
 * moving, and the rate is far too quick". Vanilla's units make that obvious in
 * hindsight: 4.0 exhaustion is one hunger point, so 20/s was five hunger points
 * a second, i.e. an empty bar in four seconds.</p>
 *
 * <p><b>Why {@code causeFoodExhaustion} is the right seam.</b> Every vanilla
 * drain - sprinting, jumping, mining, attacking, regeneration, being hit - goes
 * through this one method, and it is called with the amount the action is worth.
 * Doubling the argument therefore means "the same actions cost twice as much",
 * which is what the design asked for ("consumption follows the vanilla
 * mechanism") and, importantly, drains nothing at all while the player stands
 * still. The drink doubles a cost; it does not invent one.</p>
 *
 * <p>Server side only, because {@code causeFoodExhaustion} runs on both sides
 * and hunger is a server-owned value - doubling the client's copy would make the
 * client's predicted hunger bar disagree with the server's.</p>
 */
@Mixin(Player.class)
public abstract class ToastedHungerMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float flora$doubleExhaustion(float exhaustion) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || !self.hasEffect(ModEffects.TOASTED_CAKE)) {
            return exhaustion;
        }
        return exhaustion * 2.0f;
    }
}
