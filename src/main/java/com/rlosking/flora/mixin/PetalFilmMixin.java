package com.rlosking.flora.mixin;

import com.rlosking.flora.init.ModEffects;
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
import com.rlosking.flora.effect.PetalWalkEffect;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PetalFilmMixin {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void flora$petalFilm(BlockGetter level, BlockPos pos, CollisionContext context,
                                 CallbackInfoReturnable<VoxelShape> cir) {
        FluidState fluid = ((BlockState) (Object) this).getFluidState();
        if (!fluid.is(FluidTags.WATER) || !(context instanceof EntityCollisionContext entityContext)) {
            return;
        }
        if (!(entityContext.getEntity() instanceof LivingEntity walker) || !walker.hasEffect(ModEffects.PETALWALK)) {
            return;
        }
        if (walker.isPassenger() || walker.isShiftKeyDown() || walker.isInWater()
                || (walker instanceof Player pilot && pilot.getAbilities().flying)) {
            return;
        }
        if (walker.getDeltaMovement().y() < -PetalWalkEffect.PLUNGE_SPEED) {
            return;
        }
        float height = Math.max(fluid.getOwnHeight(), 0.05f);
        if (pos.getY() + height > walker.getY() + 0.05) {
            return;
        }
        if (!((BlockState) (Object) this).getCollisionShape(level, pos, CollisionContext.empty()).isEmpty()) {
            return;
        }
        cir.setReturnValue(Shapes.box(0.0, 0.0, 0.0, 1.0, height, 1.0));
    }
}
