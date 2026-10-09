package io.github.b1tr0n1n.signal.network;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.campaign.SiteInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Forge 1.20.1 SimpleImpl channel. Client-to-server packets carry intentions only; the server
 * recomputes everything and answers with a fresh snapshot.
 */
public final class SignalNetwork {
    public static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private SignalNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(ApplyAllocation.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ApplyAllocation::encode).decoder(ApplyAllocation::decode)
                .consumerMainThread(ApplyAllocation::handle).add();
        CHANNEL.messageBuilder(InstallFuse.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(InstallFuse::encode).decoder(InstallFuse::decode)
                .consumerMainThread(InstallFuse::handle).add();
        CHANNEL.messageBuilder(Snapshot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode)
                .consumerMainThread(Snapshot::handle).add();
    }

    public static void sendSnapshot(ServerPlayer player, PanelSnapshot snapshot, boolean open) {
        // A player without a live network channel (disconnecting, or a GameTest mock) cannot receive packets.
        if (player.connection == null || !player.connection.connection.isConnected()) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(snapshot, open));
    }

    /** Client intention: set breakers to {@code mask}, based on what was seen at {@code revision}. */
    public record ApplyAllocation(BlockPos pos, int mask, int revision) {
        void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(mask);
            buf.writeVarInt(revision);
        }

        static ApplyAllocation decode(FriendlyByteBuf buf) {
            return new ApplyAllocation(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                SiteInteractions.applyAllocation(sender, pos, PanelSnapshot.unmask(mask), revision);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client intention: fit the replacement fuse at the panel. */
    public record InstallFuse(BlockPos pos, int revision) {
        void encode(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeVarInt(revision);
        }

        static InstallFuse decode(FriendlyByteBuf buf) {
            return new InstallFuse(buf.readBlockPos(), buf.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                SiteInteractions.installFuse(sender, pos, revision);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    /** Server snapshot; {@code open} asks the client to open the panel screen, otherwise only refresh it. */
    public record Snapshot(PanelSnapshot snapshot, boolean open) {
        void encode(FriendlyByteBuf buf) {
            snapshot.encode(buf);
            buf.writeBoolean(open);
        }

        static Snapshot decode(FriendlyByteBuf buf) {
            return new Snapshot(PanelSnapshot.decode(buf), buf.readBoolean());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            // Client-only code is reached through DistExecutor so a dedicated server never loads it.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    io.github.b1tr0n1n.signal.client.ClientPacketHandlers.onSnapshot(snapshot, open));
            ctx.get().setPacketHandled(true);
        }
    }
}
