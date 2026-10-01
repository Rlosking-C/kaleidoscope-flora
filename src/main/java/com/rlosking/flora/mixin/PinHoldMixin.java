package com.rlosking.flora.mixin;

import com.rlosking.flora.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pin: a pinned mob is actually held in place, whatever kind of mover it is.
 *
 * <p><b>What was missing.</b> The pin had two mechanisms against movement - Slow
 * VI and {@link PinJumpMixin}, which refuses {@code jumpFromGround} - and both
 * are about <em>walking</em>. Testing found the consequence: "the pin has no
 * effect on flying mobs - bees, bats, ghasts, phantoms".</p>
 *
 * <ul>
 *   <li>Nothing to refuse: flyers never call {@code jumpFromGround}, so the jump
 *       block is inert on them.</li>
 *   <li>Slowness often does nothing either, because the ones that move by
 *       writing {@code deltaMovement} straight from their own move control - the
 *       ghast is the clearest case - never consult the movement-speed attribute
 *       that Slow modifies.</li>
 * </ul>
 *
 * <p><b>Why this hooks {@code Entity.move} and not {@code travel}.</b> The first
 * attempt hooked {@code LivingEntity#travel}, which looked like the right seam
 * and was not. <b>{@code FlyingMob} declares its own {@code travel}</b> and its
 * implementation calls {@code move} directly instead of chaining to
 * {@code super.travel} - so the injection never ran for the two mobs the report
 * was actually about (the ghast and the phantom). {@code Player} overrides
 * {@code travel} too.</p>
 *
 * <p>{@code Entity.move} has neither problem: it is declared in exactly one
 * class, <b>nothing in the vanilla hierarchy overrides it</b>, and every travel
 * implementation funnels into it - {@code LivingEntity.travel} calls it,
 * {@code FlyingMob.travel} calls it, {@code Player.travel} calls it. Hooking the
 * single funnel covers every mover there is, including ones added by other
 * mods.</p>
 *
 * <p><b>Not players.</b> Player movement is client-authoritative, so cancelling
 * their displacement server-side would fight the client and rubber-band them.
 * Slow VI already does the visible work there. Mobs are server-authoritative, so
 * cancelling theirs is exactly what "held" means.</p>
 *
 * <p><b>No "is it flying" test, because there is no such type.</b> Of the four
 * reported mobs only the ghast and the phantom extend {@code FlyingMob}; the bat
 * extends {@code AmbientCreature} and the bee extends {@code Animal} (it merely
 * implements {@code FlyingAnimal}). Nothing is scoped by height or by ground
 * state either - an earlier version skipped any target that was on the ground,
 * which is why a grounded ghast was still skipped. A pinned mob is pinned.</p>
 */
@Mixin(Entity.class)
public abstract class PinHoldMixin {

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void flora$holdPinned(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        // Mob, not LivingEntity: players are excluded (see the class note), and
        // Mob is where "this is an NPC whose movement the server owns" begins.
        if (self.level().isClientSide() || !(self instanceof Mob mob)) {
            return;
        }
        if (!mob.hasEffect(ModEffects.PINNED)) {
            return;
        }
        // Clearing the velocity as well as cancelling is not redundant: the
        // mob's own AI writes a fresh velocity every tick, so without this the
        // target would lurch the moment the pin ended.
        mob.setDeltaMovement(Vec3.ZERO);
        ci.cancel();
    }
}
