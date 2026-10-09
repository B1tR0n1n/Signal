package io.github.b1tr0n1n.signal.network;

import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.FuseState;
import io.github.b1tr0n1n.signal.domain.Service;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bounded, validated view of the panel's node state sent to the client. Contains no history.
 *
 * @param status translation key describing the last action's server-side result ("" for none)
 */
public record PanelSnapshot(BlockPos pos, int revision, boolean fuseInstalled, boolean isolated, int configuredMask,
                            boolean relayAligned, boolean playerHasFuse, String status) {
    private static final int MAX_STATUS = 96;

    public static int mask(Set<Service> services) {
        int m = 0;
        for (Service s : services) {
            m |= 1 << s.ordinal();
        }
        return m;
    }

    public static EnumSet<Service> unmask(int mask) {
        EnumSet<Service> out = EnumSet.noneOf(Service.class);
        for (Service s : Service.values()) {
            if ((mask & (1 << s.ordinal())) != 0) {
                out.add(s);
            }
        }
        return out;
    }

    public EnumSet<Service> configured() {
        return unmask(configuredMask);
    }

    /** Client-side read-only reconstruction for preview text. Never sent back as fact. */
    public BellwetherBus toBus() {
        return new BellwetherBus(fuseInstalled ? FuseState.INSTALLED : FuseState.DAMAGED, isolated, configured(), relayAligned, revision);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(revision);
        buf.writeBoolean(fuseInstalled);
        buf.writeBoolean(isolated);
        buf.writeVarInt(configuredMask);
        buf.writeBoolean(relayAligned);
        buf.writeBoolean(playerHasFuse);
        buf.writeUtf(status, MAX_STATUS);
    }

    public static PanelSnapshot decode(FriendlyByteBuf buf) {
        return new PanelSnapshot(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(MAX_STATUS));
    }
}
