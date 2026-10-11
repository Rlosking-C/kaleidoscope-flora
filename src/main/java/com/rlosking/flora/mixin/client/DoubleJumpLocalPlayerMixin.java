package com.rlosking.flora.mixin.client;

import com.rlosking.flora.network.FloraNetwork;
import com.rlosking.flora.init.ModEffects;
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

@Mixin(LocalPlayer.class)
public abstract class DoubleJumpLocalPlayerMixin {

    @Unique
    private int flora$jumpsLeft;

    @Unique
    private boolean flora$wasOnGround = true;

    @Unique
    private boolean flora$jumpWasHeld;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void flora$doubleJump(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        boolean onGround = self.onGround();
        MobEffectInstance dew = self.getEffect(ModEffects.DEW_CAKE);

        boolean jumpHeld = self.input.jumping;
        boolean jumpPressed = jumpHeld && !flora$jumpWasHeld;
        flora$jumpWasHeld = jumpHeld;

        if (onGround || self.onClimbable()) {
            flora$jumpsLeft = dew == null ? 0 : dew.getAmplifier() + 1;
            flora$wasOnGround = true;
            return;
        }
        boolean justLeftGround = flora$wasOnGround;
        flora$wasOnGround = false;
        if (dew == null) {
            flora$jumpsLeft = 0;
            return;
        }
        if (justLeftGround || flora$jumpsLeft <= 0) {
            return;
        }
        if (jumpPressed) {
            flora$jumpsLeft--;
            boolean stillRising = self.getDeltaMovement().y > DewJumpFeedback.RISING_VELOCITY;
            self.jumpFromGround();
            DewJumpFeedback.play(self);
            FloraNetwork.sendMidAirJump(stillRising);
        }
    }

}
