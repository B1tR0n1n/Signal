package io.github.b1tr0n1n.signal.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static io.github.b1tr0n1n.signal.domain.Service.*;
import static org.junit.jupiter.api.Assertions.*;

class BellwetherSiteTest {

    @Test
    void openingIsDeliveredOnce() {
        BellwetherSite site = new BellwetherSite();
        assertEquals(List.of(RadioCatalog.OPENING), site.begin());
        assertTrue(site.begin().isEmpty());
        assertEquals(List.of(RadioCatalog.OPENING.id()), site.radio().transcript());
    }

    @Test
    void localRestoredAcknowledgementFiresOnceWhenTransmitterActuallyRuns() {
        BellwetherSite site = new BellwetherSite();
        // Configuring the transmitter while isolated is not service: no acknowledgement yet.
        var applied = site.applyAllocation(EnumSet.of(PUMP, LOCAL_TRANSMITTER), site.bus().revision());
        assertEquals(ApplyResult.APPLIED, applied.result());
        assertTrue(applied.delivered().isEmpty());

        assertTrue(site.installFuse(true, site.bus().revision()).delivered().isEmpty());
        var closed = site.closeIsolator();
        assertEquals(IsolatorResult.CLOSED, closed.result());
        assertEquals(List.of(RadioCatalog.LOCAL_RESTORED), closed.delivered());

        // Cycling the isolator must not repeat the acknowledgement.
        site.openIsolator();
        assertTrue(site.closeIsolator().delivered().isEmpty());
        assertTrue(site.evaluateTriggers().isEmpty());
        assertEquals(1, site.radio().transcript().stream().filter(RadioCatalog.LOCAL_RESTORED.id()::equals).count());
    }

    @Test
    void replayingTranscriptAddsNoEvent() {
        BellwetherSite site = new BellwetherSite();
        site.begin();
        int events = site.history().events().size();
        for (int i = 0; i < 3; i++) {
            site.radio().transcript();
        }
        assertEquals(events, site.history().events().size());
    }

    @Test
    void failedActionsAreNotNarrativeEvents() {
        BellwetherSite site = new BellwetherSite();
        site.installFuse(false, site.bus().revision());
        site.closeIsolator();
        site.applyAllocation(EnumSet.of(PUMP, REGIONAL_RELAY), site.bus().revision());
        assertTrue(site.history().events().isEmpty());
    }

    @Test
    void historyIsOrderedAndRestorable() {
        BellwetherSite site = new BellwetherSite();
        site.begin();
        site.installFuse(true, site.bus().revision());
        site.closeIsolator();
        List<HistoryEvent> events = site.history().events();
        for (int i = 1; i < events.size(); i++) {
            assertTrue(events.get(i).sequence() > events.get(i - 1).sequence());
        }
        CampaignHistory restored = new CampaignHistory(events);
        HistoryEvent next = restored.record("x", "elias", "y", "");
        assertEquals(events.get(events.size() - 1).sequence() + 1, next.sequence());
    }

    @Test
    void unpresentedMessagesSurviveUntilShown() {
        RadioLog log = new RadioLog();
        log.deliver("a");
        log.deliver("b");
        log.markPresented("a");
        assertEquals(List.of("b"), log.unpresented());
        RadioLog restored = new RadioLog(log.transcript(), List.copyOf(log.presentedIds()));
        assertEquals(List.of("b"), restored.unpresented());
        assertFalse(restored.deliver("a"), "restored log must not redeliver");
    }
}
