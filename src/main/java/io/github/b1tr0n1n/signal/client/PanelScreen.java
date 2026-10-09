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
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 236;
    private static final int BG = 0xE0181A17;
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

    /** Fresh server snapshot. The proposal is reset to the applied state whenever the server state moved. */
    public void update(PanelSnapshot fresh) {
        boolean moved = fresh.revision() != snapshot.revision();
        snapshot = fresh;
        if (moved) {
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
        int y = top + 104;
        for (Service s : Service.values()) {
            Button b = Button.builder(toggleLabel(s), btn -> {
                if (!proposal.remove(s)) {
                    proposal.add(s);
                }
                refreshWidgets();
            }).bounds(left + 10, y, 150, 18).build();
            toggles.put(s, addRenderableWidget(b));
            y += 21;
        }
        applyButton = addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.apply"), b ->
                        SignalNetwork.CHANNEL.sendToServer(new SignalNetwork.ApplyAllocation(snapshot.pos(), PanelSnapshot.mask(proposal), snapshot.revision())))
                .bounds(left + 10, top + PANEL_H - 28, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.revert"), b -> {
            proposal.clear();
            proposal.addAll(snapshot.configured());
            refreshWidgets();
        }).bounds(left + 90, top + PANEL_H - 28, 76, 20).build());
        fuseButton = addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.install_fuse"), b ->
                        SignalNetwork.CHANNEL.sendToServer(new SignalNetwork.InstallFuse(snapshot.pos(), snapshot.revision())))
                .bounds(left + 170, top + PANEL_H - 28, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.signal.panel.close"), b -> onClose())
                .bounds(left + PANEL_W - 74, top + PANEL_H - 28, 64, 20).build());
        refreshWidgets();
    }

    private Component toggleLabel(Service s) {
        String box = proposal.contains(s) ? "[x] " : "[ ] ";
        return Component.literal(box).append(Component.translatable("service.signal." + s.id()))
                .append(Component.literal(" - " + s.demand() + " "))
                .append(Component.translatable("screen.signal.units"));
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

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
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
        g.drawString(font, Component.translatable("screen.signal.panel.proposed").withStyle(ChatFormatting.UNDERLINE), left + 10, top + 92, DIM, false);

        // ----- preview (right column) -----
        int px = left + 172;
        int py = top + 92;
        int pw = PANEL_W - 182;
        g.drawString(font, Component.translatable("screen.signal.panel.preview").withStyle(ChatFormatting.UNDERLINE), px, py, DIM, false);
        py += 12;
        for (Component c : previewLines(bus.preview(proposal), bus)) {
            for (FormattedCharSequence seq : font.split(c, pw)) {
                g.drawString(font, seq, px, py, TEXT, false);
                py += 10;
            }
            py += 2;
        }

        // ----- server status of the last action -----
        if (!snapshot.status().isEmpty()) {
            g.drawString(font, Component.translatable(snapshot.status()), left + 10, top + PANEL_H - 42, WARN, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private List<Component> previewLines(AllocationPreview p, BellwetherBus bus) {
        List<Component> out = new ArrayList<>();
        if (p.requested().isEmpty()) {
            out.add(Component.translatable("screen.signal.preview.none"));
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
