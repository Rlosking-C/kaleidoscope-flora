package com.rlosking.flora;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The mod's one packet: "I just used an extra mid-air jump" - and <em>when</em>.
 *
 * <p><b>Why this exists at all.</b> The Dew Flower Cake's landing shockwave has
 * to tell a deliberate double jump from the extra jumps a player burns while
 * bunny-hopping, and no height threshold can do it. The reason is physical:
 * {@code LivingEntity#jumpFromGround} sets the vertical velocity to the jump
 * power <b>unconditionally</b>, so pressing again early does not give a smaller
 * boost - it gives a <em>full</em> one from wherever the player had got to.
 * Measured against the arc, that puts a spam hop at about 2.0 blocks and a
 * double jump taken at the top of the first jump at about 2.5, while a plain
 * single jump reaches 1.25. The two cases the design cares about sit less than
 * half a block apart, on opposite sides of the design's own two-block floor.</p>
 *
 * <p>Earlier attempts tried to infer the case on the server from the position
 * stream and each failed for its own reason:</p>
 *
 * <ul>
 *   <li><b>Per-tick rise accelerating</b> - a dropped movement packet makes one
 *       step cover two ticks of rise, which looks exactly like a jump impulse.
 *       Testing caught it: flat single jumps occasionally fired.</li>
 *   <li><b>Peak higher than one jump can reach</b> - immune to packet loss and
 *       it never false-fires, but it cannot see a second jump taken late, and it
 *       <em>does</em> fire on an early one, because an early one also clears the
 *       barrier. Testing caught both ends.</li>
 * </ul>
 *
 * <p>So the client reports, and it reports the one thing the server cannot
 * reconstruct: <b>whether the player was still shooting upward when they
 * pressed.</b> That is the difference between "I meant to double jump" and "I was
 * mashing the key", and it costs one boolean.</p>
 *
 * <p><b>Nothing else is sent.</b> The fact of the jump, plus that flag; the
 * server still decides its own damage and knockback, so the worst a client can
 * claim is "I jumped", which every client is allowed to do anyway.</p>
 */
@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraNetwork {

    /** Channel version, as NeoForge requires for a payload registrar. */
    private static final String VERSION = "1";

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(MidAirJumpPayload.TYPE, MidAirJumpPayload.CODEC,
                FloraNetwork::handleMidAirJump);
    }

    /**
     * Called from the client mixin at the moment it spends a jump.
     *
     * @param stillRising whether the player's vertical velocity was still upward
     *                    when the jump was spent - i.e. whether this was a mash
     *                    rather than a deliberate second jump
     */
    public static void sendMidAirJump(boolean stillRising) {
        PacketDistributor.sendToServer(new MidAirJumpPayload(stillRising));
    }

    private static void handleMidAirJump(MidAirJumpPayload payload, IPayloadContext context) {
        // enqueueWork: payload handlers may run off the game thread, and this
        // touches per-player state that the tick loop also touches.
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                FloraEvents.noteMidAirJump(player, payload.stillRising());
            }
        });
    }

    /**
     * The payload. One boolean, so the codec is hand-written rather than
     * {@code StreamCodec.unit} - that helper is typed to {@code ByteBuf}, which
     * the registrar's {@code RegistryFriendlyByteBuf} is not a subtype of.
     */
    public record MidAirJumpPayload(boolean stillRising) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<MidAirJumpPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(KaleidoscopeFlora.MOD_ID, "mid_air_jump"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MidAirJumpPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeBoolean(payload.stillRising()),
                        buf -> new MidAirJumpPayload(buf.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private FloraNetwork() {
    }
}
