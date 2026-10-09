package io.github.b1tr0n1n.signal.domain;

import java.util.ArrayList;
import java.util.List;

/** Append-only canonical history for one campaign timeline. */
public final class CampaignHistory {
    private final List<HistoryEvent> events = new ArrayList<>();
    private long nextSequence = 1;

    public CampaignHistory() {
    }

    public CampaignHistory(List<HistoryEvent> restored) {
        for (HistoryEvent e : restored) {
            events.add(e);
            nextSequence = Math.max(nextSequence, e.sequence() + 1);
        }
    }

    public HistoryEvent record(String type, String actor, String target, String payload) {
        HistoryEvent e = new HistoryEvent(nextSequence++, type, actor, target, payload);
        events.add(e);
        return e;
    }

    public List<HistoryEvent> events() {
        return List.copyOf(events);
    }

    public boolean contains(String type) {
        for (HistoryEvent e : events) {
            if (e.type().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
