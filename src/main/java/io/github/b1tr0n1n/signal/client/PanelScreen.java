package io.github.b1tr0n1n.signal.client;

import io.github.b1tr0n1n.signal.domain.AllocationPreview;
import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.Service;
import io.github.b1tr0n1n.signal.network.PanelSnapshot;
import io.github.b1tr0n1n.signal.network.SignalNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Distribution panel DP-1. Shows the applied (current) state separately from the proposal being
 * edited, previews the result with the same domain rules the server uses, and submits only an
 * intention (requested services + the revision it was based on). Cancel never mutates anything.
 *
 * <p>Fully keyboard operable: Tab/Shift+Tab move between buttons, Enter/Space activates, Esc closes.
 */
public class PanelScreen extends Screen {
    private static final int PANEL_W = 372;
    private static final int PANEL_H = 240;
    /** Left column: applied state, proposal toggles, last server status. Right column: preview. */
    private static final int LEFT_W = 176;
    private static final int RIGHT_X = LEFT_W + 20;
    private static final int BG = 0xF4181A17;
    private static final String STATUS_STALE = "screen.signal.panel.status.stale";
    private static final int FRAME = 0xFF5A5F45;
    private static final int TEXT = 0xFFD8CFB2;
    private static final int DIM = 0xFF8C8670;
    private static final int GOOD = 0xFF9FD08A;
    private static final int WARN = 0xFFE8A040;

    private PanelSnapshot snapshot;
    private final EnumSet<Service> proposal;
    private final Map<Service, Button> toggles = new EnumMap<>(Service.class);
    private Button applyButton;
    private Button fuseButton;
    private int left;
    private int top;

    public PanelScreen(PanelSnapshot snapshot) {
        super(Component.translatable("screen.signal.panel.title"));
        this.snapshot = snapshot;
        this.proposal = snapshot.configured();
    }

    public boolean isFor(BlockPos pos) {
        return snapshot.pos().equals(pos);
    }

    /**
     * Fresh server snapshot. The player's unapplied proposal survives their own actions (e.g. fitting the
     * fuse) and is re-previewed against the new state; after a rejected stale apply it is reset to what is
     * actually applied, so the player sees the truth before trying again.
     */
    public void update(PanelSnapshot fresh) {
        snapshot = fresh;
        if (STATUS_STALE.equals(fresh.status())) {
            proposal.clear();
            proposal.addAll(fresh.configured());
        }
        refreshWidgets();
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        toggles.clear();
        int y = top + 108;
        for (Service s : Service.values()) {
            Button b = Button.builder(toggleLabel(s), btn -> {
                if (!proposal.remove(s)) {
                    proposal.add(s);
                }
                refreshWidgets();
            }).bounds(left + 10, y, LEFT_W, 18).build();
            toggles.put(s, addRenderableWidget(b));
            y += 21;
        }
        applyButton = addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.apply"), b ->
                        SignalNetwork.sendToServer(new SignalNetwork.ApplyAllocation(snapshot.pos(), PanelSnapshot.mask(proposal), snapshot.revision())))
                .bounds(left + 10, top + PANEL_H - 28, 84, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.revert"), b -> {
            proposal.clear();
            proposal.addAll(snapshot.configured());
            refreshWidgets();
        }).bounds(left + 102, top + PANEL_H - 28, 84, 20).build());
        fuseButton = addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.install_fuse"), b ->
                        SignalNetwork.sendToServer(new SignalNetwork.InstallFuse(snapshot.pos(), snapshot.revision())))
                .bounds(left + RIGHT_X, top + PANEL_H - 28, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.close"), b -> onClose())
                .bounds(left + PANEL_W - 74, top + PANEL_H - 28, 64, 20).build());
        refreshWidgets();
    }

    private Component toggleLabel(Service s) {
        String box = proposal.contains(s) ? "[x] " : "[ ] ";
        return Component.literal(box).append(Component.translatable("service.signal." + s.id()))
                .append(Component.literal(" - "))
                .append(units(s.demand()));
    }

    private static Component units(int n) {
        return Component.translatable(n == 1 ? "screen.signal.units.one" : "screen.signal.units.many", n);
    }

    private void refreshWidgets() {
        if (applyButton == null) {
            return;
        }
        for (Map.Entry<Service, Button> e : toggles.entrySet()) {
            e.getValue().setMessage(toggleLabel(e.getKey()));
        }
        AllocationPreview p = snapshot.toBus().preview(proposal);
        applyButton.active = p.valid() && !proposal.equals(snapshot.configured());
        fuseButton.visible = !snapshot.fuseInstalled();
        fuseButton.active = snapshot.isolated() && snapshot.playerHasFuse();
    }

    /**
     * 1.21: {@link Screen#render} draws the background first and the widgets after it, so the panel body
     * (frame, applied state, preview, status) is drawn here, between the two.
     */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, BG);
        g.renderOutline(left, top, PANEL_W, PANEL_H, FRAME);
        g.drawString(font, title, left + 10, top + 8, TEXT, false);
        g.hLine(left + 8, left + PANEL_W - 8, top + 20, FRAME);

        BellwetherBus bus = snapshot.toBus();
        // ----- current (applied) state -----
        int y = top + 26;
        g.drawString(font, Component.translatable("screen.signal.panel.current").withStyle(ChatFormatting.UNDERLINE), left + 10, y, DIM, false);
        y += 12;
        g.drawString(font, Component.translatable("screen.signal.panel.supply", BellwetherBus.GENERATOR_SUPPLY,
                Component.translatable(bus.isolated() ? "state.signal.isolated" : "state.signal.closed")), left + 10, y, bus.busLive() ? GOOD : WARN, false);
        y += 11;
        g.drawString(font, Component.translatable(snapshot.fuseInstalled() ? "screen.signal.panel.fuse_ok" : "screen.signal.panel.fuse_damaged"),
                left + 10, y, snapshot.fuseInstalled() ? GOOD : WARN, false);
        y += 11;
        for (Service s : Service.values()) {
            boolean running = bus.isRunning(s);
            boolean set = bus.configured().contains(s);
            String stateKey = running ? "state.signal.running" : set ? "state.signal.set_no_power" : "state.signal.off";
            MutableComponent line = Component.literal(running ? "■ " : "□ ")
                    .append(Component.translatable("service.signal." + s.id())).append(": ")
                    .append(Component.translatable(stateKey));
            if (s == Service.REGIONAL_RELAY && running) {
                line.append(" - ").append(Component.translatable(bus.relayReachable() ? "state.signal.reachable" : "state.signal.no_route"));
            }
            g.drawString(font, line, left + 16, y, running ? GOOD : DIM, false);
            y += 10;
        }

        // ----- proposal -----
        g.drawString(font, Component.translatable("screen.signal.panel.proposed").withStyle(ChatFormatting.UNDERLINE), left + 10, top + 96, DIM, false);
        g.vLine(left + RIGHT_X - 8, top + 24, top + PANEL_H - 36, FRAME);

        // ----- preview (right column, full height) -----
        int px = left + RIGHT_X;
        int py = top + 26;
        int pw = PANEL_W - RIGHT_X - 10;
        g.drawString(font, Component.translatable("screen.signal.panel.preview").withStyle(ChatFormatting.UNDERLINE), px, py, DIM, false);
        py += 12;
        for (Component c : previewLines(bus.preview(proposal), bus)) {
            for (FormattedCharSequence seq : font.split(c, pw)) {
                g.drawString(font, seq, px, py, TEXT, false);
                py += 10;
            }
            py += 2;
        }

        // ----- server status of the last action (left column, wrapped) -----
        if (!snapshot.status().isEmpty()) {
            int sy = top + 176;
            for (FormattedCharSequence seq : font.split(Component.translatable(snapshot.status()), LEFT_W)) {
                g.drawString(font, seq, left + 10, sy, WARN, false);
                sy += 10;
            }
        }
    }

    private List<Component> previewLines(AllocationPreview p, BellwetherBus bus) {
        List<Component> out = new ArrayList<>();
        if (p.requested().isEmpty()) {
            out.add(Component.translatable("screen.signal.preview.none"));
            if (!snapshot.fuseInstalled()) {
                out.add(Component.translatable(snapshot.playerHasFuse() ? "screen.signal.preview.fuse_ready" : "screen.signal.preview.fuse_needed"));
            }
            return out;
        }
        MutableComponent breakdown = Component.empty();
        boolean first = true;
        for (Service s : Service.values()) {
            if (p.requested().contains(s)) {
                if (!first) {
                    breakdown.append(" + ");
                }
                breakdown.append(Component.translatable("service.signal." + s.id())).append(" " + s.demand());
                first = false;
            }
        }
        out.add(Component.translatable("screen.signal.preview.sum", breakdown, p.required(), p.capacity()));
        if (!p.valid()) {
            out.add(Component.translatable("screen.signal.preview.over", p.shortfall()).withStyle(ChatFormatting.GOLD));
        } else if (!p.busLive()) {
            out.add(Component.translatable(snapshot.fuseInstalled() ? "screen.signal.preview.isolated" : "screen.signal.preview.fault"));
        } else {
            out.add(Component.translatable("screen.signal.preview.ok", p.capacity() - p.required()));
        }
        if (p.relayWithoutRoute()) {
            out.add(Component.translatable("screen.signal.preview.relay_no_route"));
        }
        if (!snapshot.fuseInstalled()) {
            out.add(Component.translatable(snapshot.playerHasFuse() ? "screen.signal.preview.fuse_ready" : "screen.signal.preview.fuse_needed"));
        }
        return out;
    }

    @Override
    public boolean isPauseScreen() {
        // Correctness never depends on pausing; keeping the world running lets packets flow normally.
        return false;
    }
}
