package io.github.b1tr0n1n.signal.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which authored messages have been delivered (entered the transcript) and which the player has
 * actually been shown live. Delivery is idempotent: a one-shot message enters the log once, and
 * replaying the transcript never creates a new delivery.
 */
public final class RadioLog {
    private final LinkedHashSet<String> delivered = new LinkedHashSet<>();
    private final LinkedHashSet<String> presented = new LinkedHashSet<>();

    public RadioLog() {
    }

    public RadioLog(List<String> delivered, List<String> presented) {
        this.delivered.addAll(delivered);
        this.presented.addAll(presented);
    }

    /** @return true only the first time the message is delivered */
    public boolean deliver(String messageId) {
        return delivered.add(messageId);
    }

    public boolean isDelivered(String messageId) {
        return delivered.contains(messageId);
    }

    /** Delivered messages the participant has not yet seen live (e.g. delivered while offline). */
    public List<String> unpresented() {
        List<String> out = new ArrayList<>();
        for (String id : delivered) {
            if (!presented.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    public void markPresented(String messageId) {
        if (delivered.contains(messageId)) {
            presented.add(messageId);
        }
    }

    /** Ordered transcript ids. Reading it has no side effects. */
    public List<String> transcript() {
        return List.copyOf(delivered);
    }

    public Set<String> presentedIds() {
        return Collections.unmodifiableSet(presented);
    }
}
