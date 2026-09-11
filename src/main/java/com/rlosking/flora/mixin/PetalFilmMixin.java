package com.rlosking.flora.mixin;

import com.rlosking.flora.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The petal film as real geometry. Vanilla already turns the fluid under a
 * stander-on-fluids entity into a collision box (the strider's lava floor is
 * resolved through this exact query); this intercept extends that native
 * path to the water under a PetalWalk drinker: the cell gains a floor whose
 * top matches the fluid surface, and everything downstream - support,
 * jumping, auto-stepping onto the bank, wading ashore - is ordinary vanilla
 * physics. No position pinning, no teleports, nothing that can desync
 * between client and server.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PetalFilmMixin {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void flora$petalFilm(BlockGetter level, BlockPos pos, CollisionContext context,
                                 CallbackInfoReturnable<VoxelShape> cir) {
        // Hot path: every swept block of every moving entity lands here, so
        // the cheap rejections come first and the effect check is last.
        FluidState fluid = ((BlockState) (Object) this).getFluidState();
        if (!fluid.is(FluidTags.WATER) || !(context instanceof EntityCollisionContext entityContext)) {
            return;
        }
        if (!(entityContext.getEntity() instanceof LivingEntity walker) || !walker.hasEffect(ModEffects.PETALWALK)) {
            return;
        }
        // Deliberate dips: riding, sneaking (the dive key) and being
        // submerged all stand the film back and hand the drinker to the
        // fluid, as does creative flight.
        if (walker.isPassenger() || walker.isShiftKeyDown() || walker.isInWater()
                || (walker instanceof Player pilot && pilot.getAbilities().flying)) {
            return;
        }
        // A plunge from the air sinks: while descending faster than a stride
        // jump lands, the film must not engage at all.
        if (walker.getDeltaMovement().y() < -ModEffects.PetalWalkEffect.PLUNGE_SPEED) {
            return;
        }
        float height = Math.max(fluid.getOwnHeight(), 0.05f);
        // Only the layer at the feet (and anything deeper) carries the film:
        // water whose surface would top the drinker never becomes a wall.
        if (pos.getY() + height > walker.getY() + 0.05) {
            return;
        }
        // Kelp and seagrass carry water yet have no geometry of their own -
        // the film bridges them. A cell whose block DOES have its own
        // collision (a waterlogged slab, a lily pad) keeps that shape: the
        // film never replaces real geometry. The probe runs with an empty
        // context, so it cannot re-enter this intercept.
        if (!((BlockState) (Object) this).getCollisionShape(level, pos, CollisionContext.empty()).isEmpty()) {
            return;
        }
        cir.setReturnValue(Shapes.box(0.0, 0.0, 0.0, 1.0, height, 1.0));
    }
}
