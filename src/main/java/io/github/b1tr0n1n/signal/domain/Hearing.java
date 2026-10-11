package io.github.b1tr0n1n.signal.domain;

/**
 * Whether a noise reaches a listener. Sound carries further in the still, damp night air of the islands:
 * at night a noise is heard out to 1.5 times its daytime radius.
 */
public final class Hearing {
    public static final double NIGHT_FACTOR = 1.5;

    private Hearing() {
    }

    public static boolean hears(double distance, int radius, boolean night) {
        return distance <= radius * (night ? NIGHT_FACTOR : 1.0);
    }
}
