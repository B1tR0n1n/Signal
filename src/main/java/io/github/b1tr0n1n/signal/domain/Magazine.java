package io.github.b1tr0n1n.signal.domain;

/**
 * Round accounting for a gun: what is loaded and what is carried. Rounds are never created or lost here;
 * a shot spends one loaded round, a load moves one carried round into the gun. Counts read from item data
 * are clamped, never trusted.
 */
public final class Magazine {
    private Magazine() {
    }

    /** Outcome of pulling the trigger. */
    public record Shot(boolean fired, int loaded) {
    }

    /** Outcome of loading one round. */
    public record Load(boolean loadedOne, int loaded, int carried) {
    }

    public static Shot fire(int loaded) {
        int n = Math.max(0, loaded);
        return n == 0 ? new Shot(false, 0) : new Shot(true, n - 1);
    }

    public static Load loadOne(int loaded, int capacity, int carried) {
        int n = Math.max(0, Math.min(capacity, loaded));
        int c = Math.max(0, carried);
        if (n >= capacity || c == 0) {
            return new Load(false, n, c);
        }
        return new Load(true, n + 1, c - 1);
    }
}
