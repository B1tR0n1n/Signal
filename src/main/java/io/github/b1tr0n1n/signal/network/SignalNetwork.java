package io.github.b1tr0n1n.signal.network;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.campaign.SiteInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 1.21.1 payload networking. Client-to-server payloads carry intentions only; the server
 * recomputes everything and answers with a fresh snapshot. Handlers run on the main thread (the
 * registrar default).
 */
public final class SignalNetwork {
    public static final String PROTOCOL = "1";

    private SignalNetwork() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, path);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL);
        r.playToServer(ApplyAllocation.TYPE, ApplyAllocation.CODEC, ApplyAllocation::handle);
        r.playToServer(InstallFuse.TYPE, InstallFuse.CODEC, InstallFuse::handle);
        r.playToServer(Reload.TYPE, Reload.CODEC, Reload::handle);
        // The handler body references client-only code; it is only ever invoked on the physical client.
        r.playToClient(Snapshot.TYPE, Snapshot.CODEC, (payload, ctx) ->
                io.github.b1tr0n1n.signal.client.ClientPacketHandlers.onSnapshot(payload.snapshot(), payload.open()));
    }

    public static void sendSnapshot(ServerPlayer player, PanelSnapshot snapshot, boolean open) {
        // A player without a live network channel (disconnecting, or a GameTest fake player) cannot receive payloads.
        if (player.connection == null || !player.connection.getConnection().isConnected()) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new Snapshot(snapshot, open));
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    /** Client intention: set breakers to {@code mask}, based on what was seen at {@code revision}. */
    public record ApplyAllocation(BlockPos pos, int mask, int revision) implements CustomPacketPayload {
        public static final Type<ApplyAllocation> TYPE = new Type<>(id("apply_allocation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ApplyAllocation> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ApplyAllocation::pos,
                ByteBufCodecs.VAR_INT, ApplyAllocation::mask,
                ByteBufCodecs.VAR_INT, ApplyAllocation::revision,
                ApplyAllocation::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(ApplyAllocation p, IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sender) {
                SiteInteractions.applyAllocation(sender, p.pos, PanelSnapshot.unmask(p.mask), p.revision);
            }
        }
    }

    /** Client intention: fit the replacement fuse at the panel. */
    public record InstallFuse(BlockPos pos, int revision) implements CustomPacketPayload {
        public static final Type<InstallFuse> TYPE = new Type<>(id("install_fuse"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InstallFuse> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, InstallFuse::pos,
                ByteBufCodecs.VAR_INT, InstallFuse::revision,
                InstallFuse::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(InstallFuse p, IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sender) {
                SiteInteractions.installFuse(sender, p.pos, p.revision);
            }
        }
    }

    /** The reload key: the server checks the gun and the pockets itself. */
    public record Reload() implements CustomPacketPayload {
        public static final Reload INSTANCE = new Reload();
        public static final Type<Reload> TYPE = new Type<>(id("reload"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Reload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Reload p, IPayloadContext ctx) {
            if (ctx.player() instanceof ServerPlayer sender) {
                io.github.b1tr0n1n.signal.survival.Reloading.start(sender);
            }
        }
    }

    /** Server snapshot; {@code open} asks the client to open the panel screen, otherwise only refresh it. */
    public record Snapshot(PanelSnapshot snapshot, boolean open) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(id("panel_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.composite(
                PanelSnapshot.STREAM_CODEC, Snapshot::snapshot,
                ByteBufCodecs.BOOL, Snapshot::open,
                Snapshot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
