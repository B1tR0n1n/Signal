package io.github.b1tr0n1n.signal.client;

import io.github.b1tr0n1n.signal.network.PanelSnapshot;
import net.minecraft.client.Minecraft;

/** Physical-client only. Referenced from packets exclusively through DistExecutor. */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void onSnapshot(PanelSnapshot snapshot, boolean open) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PanelScreen screen && screen.isFor(snapshot.pos())) {
            screen.update(snapshot);
        } else if (open) {
            mc.setScreen(new PanelScreen(snapshot));
        }
    }
}
