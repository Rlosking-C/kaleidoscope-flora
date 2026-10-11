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

@EventBusSubscriber(modid = KaleidoscopeFlora.MOD_ID)
public final class FloraNetwork {

    private static final String VERSION = "1";

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(MidAirJumpPayload.TYPE, MidAirJumpPayload.CODEC,
                FloraNetwork::handleMidAirJump);
    }

    public static void sendMidAirJump(boolean stillRising) {
        PacketDistributor.sendToServer(new MidAirJumpPayload(stillRising));
    }

    private static void handleMidAirJump(MidAirJumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                FloraEvents.noteMidAirJump(player, payload.stillRising());
            }
        });
    }

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
