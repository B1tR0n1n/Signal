package io.github.b1tr0n1n.signal.domain;

/**
 * How many of the Drawn the islands allow. They are meant to be rare: a night should hold one or two close
 * enough to matter, not a crowd. The 2026-10-11 playtest found 36 loaded around a single player because the
 * old rule only capped each 48-block patch; this budget caps per player over the whole spawn range.
 */
public final class DrawnBudget {
    /** At most this many within {@link #NEAR_RADIUS} of the spawn point (which is always near a player). */
    public static final int MAX_NEAR = 2;
    public static final double NEAR_RADIUS = 96;
    /** At most this many per player within {@link #RANGE} (the natural spawn and despawn range) of that player. */
    public static final int MAX_LOADED_PER_PLAYER = 4;
    public static final double RANGE = 128;
    /** At dawn a Drawn with no player this close slips away; one being watched stays until it is not. */
    public static final double DAWN_WITNESS_RADIUS = 32;

    private DrawnBudget() {
    }

    public static boolean allowsNaturalSpawn(int loaded, int near, int players) {
        return near < MAX_NEAR && loaded < MAX_LOADED_PER_PLAYER * Math.max(1, players);
    }

    public static boolean retreatsAtDawn(boolean day, boolean hunting, double nearestPlayer) {
        return day && !hunting && nearestPlayer > DAWN_WITNESS_RADIUS;
    }
}
