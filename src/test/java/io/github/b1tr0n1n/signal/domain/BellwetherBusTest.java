package io.github.b1tr0n1n.signal.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static io.github.b1tr0n1n.signal.domain.Service.*;
import static org.junit.jupiter.api.Assertions.*;

class BellwetherBusTest {

    /** Repaired bus with the isolator closed: 6 units live. */
    private static BellwetherBus liveBus() {
        BellwetherBus bus = new BellwetherBus();
        assertEquals(RepairResult.INSTALLED, bus.installFuse(true, bus.revision()));
        assertEquals(IsolatorResult.CLOSED, bus.closeIsolator());
        return bus;
    }

    @Test
    void freshScenarioIsFaultedAndIsolated() {
        BellwetherBus bus = new BellwetherBus();
        assertEquals(FuseState.DAMAGED, bus.fuse());
        assertTrue(bus.isolated());
        assertFalse(bus.busLive());
        assertEquals(0, bus.availableSupply());
        assertTrue(bus.configured().isEmpty());
    }

    @Test
    void pumpPlusLocalTransmitterRunsOnSixUnits() {
        BellwetherBus bus = liveBus();
        AllocationPreview p = bus.preview(EnumSet.of(PUMP, LOCAL_TRANSMITTER));
        assertTrue(p.valid());
        assertEquals(6, p.required());
        assertEquals(6, p.available());
        assertEquals(ApplyResult.APPLIED, bus.apply(EnumSet.of(PUMP, LOCAL_TRANSMITTER), bus.revision()));
        assertTrue(bus.isRunning(PUMP));
        assertTrue(bus.isRunning(LOCAL_TRANSMITTER));
        assertFalse(bus.isRunning(REGIONAL_RELAY));
    }

    @Test
    void pumpPlusRelayIsRejectedWithoutReserve() {
        BellwetherBus bus = liveBus();
        AllocationPreview p = bus.preview(EnumSet.of(PUMP, REGIONAL_RELAY));
        assertFalse(p.valid());
        assertEquals(8, p.required());
        assertEquals(2, p.shortfall());
        int before = bus.revision();
        assertEquals(ApplyResult.OVER_CAPACITY, bus.apply(EnumSet.of(PUMP, REGIONAL_RELAY), before));
        assertTrue(bus.configured().isEmpty(), "rejected apply must not change configuration");
        assertEquals(before, bus.revision());
    }

    @Test
    void allThreeServicesAreRejected() {
        BellwetherBus bus = liveBus();
        assertEquals(ApplyResult.OVER_CAPACITY, bus.apply(EnumSet.allOf(Service.class), bus.revision()));
    }

    @Test
    void misalignedRelayIsPoweredButUnreachable() {
        BellwetherBus bus = liveBus();
        assertEquals(ApplyResult.APPLIED, bus.apply(EnumSet.of(REGIONAL_RELAY, LOCAL_TRANSMITTER), bus.revision()));
        assertTrue(bus.isRunning(REGIONAL_RELAY), "4 units fit, so the relay has power");
        assertFalse(bus.relayReachable(), "power alone does not align the antenna");
        assertTrue(bus.preview(EnumSet.of(REGIONAL_RELAY)).relayWithoutRoute());
        bus.setRelayAligned(true);
        assertTrue(bus.relayReachable());
    }

    @Test
    void alignedRelayWithoutPowerIsUnreachable() {
        BellwetherBus bus = new BellwetherBus();
        bus.setRelayAligned(true);
        assertEquals(ApplyResult.APPLIED, bus.apply(EnumSet.of(REGIONAL_RELAY), bus.revision()));
        assertFalse(bus.isRunning(REGIONAL_RELAY), "bus is isolated and faulted");
        assertFalse(bus.relayReachable());
    }

    @Test
    void configurationIsAllowedWhileIsolatedAndStartsWhenSupplyReturns() {
        BellwetherBus bus = new BellwetherBus();
        assertEquals(ApplyResult.APPLIED, bus.apply(EnumSet.of(PUMP, LOCAL_TRANSMITTER), bus.revision()));
        assertFalse(bus.isRunning(PUMP));
        bus.installFuse(true, bus.revision());
        bus.closeIsolator();
        assertTrue(bus.isRunning(PUMP));
    }

    @Test
    void repeatedRepairConsumesOnlyOnce() {
        BellwetherBus bus = new BellwetherBus();
        int rev = bus.revision();
        assertEquals(RepairResult.INSTALLED, bus.installFuse(true, rev));
        // The duplicate packet carries the same (now stale) revision; it must still not install twice.
        assertEquals(RepairResult.ALREADY_INSTALLED, bus.installFuse(true, rev));
        assertEquals(RepairResult.ALREADY_INSTALLED, bus.installFuse(true, bus.revision()));
    }

    @Test
    void repairWithoutFuseFailsWithoutChange() {
        BellwetherBus bus = new BellwetherBus();
        assertEquals(RepairResult.NO_FUSE, bus.installFuse(false, bus.revision()));
        assertEquals(FuseState.DAMAGED, bus.fuse());
        assertEquals(0, bus.revision());
    }

    @Test
    void repairOnLiveSupplyIsRefused() {
        // Unreachable through normal play (the isolator refuses to close on a damaged fuse) but the
        // rule must hold for any restored state.
        BellwetherBus bus = new BellwetherBus(FuseState.DAMAGED, false, Set.of(), false, 3);
        assertEquals(RepairResult.SUPPLY_LIVE, bus.installFuse(true, 3));
        assertEquals(FuseState.DAMAGED, bus.fuse());
    }

    @Test
    void isolatorRefusesToCloseOnDamagedFuse() {
        BellwetherBus bus = new BellwetherBus();
        assertEquals(IsolatorResult.REFUSED_FUSE_DAMAGED, bus.closeIsolator());
        assertTrue(bus.isolated());
        assertFalse(bus.busLive());
    }

    @Test
    void reIsolatingDropsPowerButKeepsConfiguration() {
        BellwetherBus bus = liveBus();
        bus.apply(EnumSet.of(PUMP), bus.revision());
        assertEquals(IsolatorResult.OPENED, bus.openIsolator());
        assertFalse(bus.isRunning(PUMP));
        assertEquals(EnumSet.of(PUMP), bus.configured());
        assertEquals(IsolatorResult.ALREADY_OPEN, bus.openIsolator());
    }

    @Test
    void staleSnapshotDoesNotOverwriteNewerConfiguration() {
        BellwetherBus bus = liveBus();
        int screenARevision = bus.revision();
        int screenBRevision = bus.revision();
        assertEquals(ApplyResult.APPLIED, bus.apply(EnumSet.of(PUMP, LOCAL_TRANSMITTER), screenARevision));
        assertEquals(ApplyResult.STALE_REVISION, bus.apply(EnumSet.of(LOCAL_TRANSMITTER), screenBRevision));
        assertEquals(EnumSet.of(PUMP, LOCAL_TRANSMITTER), bus.configured());
    }

    @Test
    void unchangedApplyDoesNotConsumeRevision() {
        BellwetherBus bus = liveBus();
        bus.apply(EnumSet.of(PUMP), bus.revision());
        int rev = bus.revision();
        assertEquals(ApplyResult.UNCHANGED, bus.apply(EnumSet.of(PUMP), rev));
        assertEquals(rev, bus.revision());
    }

    @Test
    void restoredStateRoundTripsThroughConstructor() {
        BellwetherBus a = liveBus();
        a.apply(EnumSet.of(PUMP, LOCAL_TRANSMITTER), a.revision());
        BellwetherBus b = new BellwetherBus(a.fuse(), a.isolated(), a.configured(), a.relayAligned(), a.revision());
        assertEquals(a.configured(), b.configured());
        assertEquals(a.busLive(), b.busLive());
        assertEquals(a.revision(), b.revision());
        assertTrue(b.isRunning(PUMP));
    }
}
