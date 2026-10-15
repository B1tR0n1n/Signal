package io.github.b1tr0n1n.signal.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrawnBudgetTest {
    @Test
    void aQuietIslandAllowsASpawn() {
        assertTrue(DrawnBudget.allowsNaturalSpawn(0, 0, 1));
    }

    @Test
    void neverMoreThanTwoCloseToAPlayer() {
        assertTrue(DrawnBudget.allowsNaturalSpawn(1, 1, 1));
        assertFalse(DrawnBudget.allowsNaturalSpawn(2, 2, 1));
    }

    @Test
    void theLoadedCapIsPerPlayerNotPerPatch() {
        // Night one of the 2026-10-11 playtest: 36 loaded around one player. The cap is 4 for one player.
        assertTrue(DrawnBudget.allowsNaturalSpawn(3, 0, 1));
        assertFalse(DrawnBudget.allowsNaturalSpawn(4, 0, 1));
        assertTrue(DrawnBudget.allowsNaturalSpawn(4, 0, 2));
        assertFalse(DrawnBudget.allowsNaturalSpawn(8, 0, 2));
    }

    @Test
    void anEmptyServerStillCountsAsOnePlayer() {
        assertFalse(DrawnBudget.allowsNaturalSpawn(4, 0, 0));
    }

    @Test
    void atDawnTheyLeaveOnlyWhenNoOneIsClose() {
        assertTrue(DrawnBudget.retreatsAtDawn(true, false, DrawnBudget.DAWN_WITNESS_RADIUS + 1));
        assertFalse(DrawnBudget.retreatsAtDawn(true, false, DrawnBudget.DAWN_WITNESS_RADIUS - 1));
        assertFalse(DrawnBudget.retreatsAtDawn(true, true, 1000), "a Drawn on a hunt keeps hunting");
        assertFalse(DrawnBudget.retreatsAtDawn(false, false, 1000), "nothing leaves at night");
    }
}
