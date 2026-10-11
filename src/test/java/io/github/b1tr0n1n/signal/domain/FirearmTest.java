package io.github.b1tr0n1n.signal.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FirearmTest {
    private static final FirearmSpec GUN = FirearmSpec.REVOLVER;

    @Test
    void firingSpendsExactlyOneRound() {
        Magazine.Shot s = Magazine.fire(GUN.capacity());
        assertTrue(s.fired());
        assertEquals(GUN.capacity() - 1, s.loaded());
    }

    @Test
    void emptyGunClicksAndStaysEmpty() {
        Magazine.Shot s = Magazine.fire(0);
        assertFalse(s.fired());
        assertEquals(0, s.loaded());
    }

    @Test
    void reloadMovesOneRoundFromWhatIsCarried() {
        Magazine.Load l = Magazine.loadOne(2, GUN.capacity(), 5);
        assertTrue(l.loadedOne());
        assertEquals(3, l.loaded());
        assertEquals(4, l.carried());
    }

    @Test
    void reloadStopsWhenFull() {
        Magazine.Load l = Magazine.loadOne(GUN.capacity(), GUN.capacity(), 5);
        assertFalse(l.loadedOne());
        assertEquals(GUN.capacity(), l.loaded());
        assertEquals(5, l.carried());
    }

    @Test
    void reloadNeedsAmmunition() {
        Magazine.Load l = Magazine.loadOne(1, GUN.capacity(), 0);
        assertFalse(l.loadedOne());
        assertEquals(1, l.loaded());
        assertEquals(0, l.carried());
    }

    @Test
    void roundsAreConservedOverAFullCycle() {
        int loaded = 0, carried = 9, fired = 0;
        for (int i = 0; i < 20; i++) {
            Magazine.Load l = Magazine.loadOne(loaded, GUN.capacity(), carried);
            loaded = l.loaded();
            carried = l.carried();
        }
        for (int i = 0; i < 20; i++) {
            Magazine.Shot s = Magazine.fire(loaded);
            loaded = s.loaded();
            fired += s.fired() ? 1 : 0;
        }
        assertEquals(GUN.capacity(), fired);
        assertEquals(9, fired + loaded + carried);
    }

    @Test
    void corruptCountsAreClampedNotTrusted() {
        assertEquals(GUN.capacity(), Magazine.loadOne(99, GUN.capacity(), 3).loaded());
        assertFalse(Magazine.fire(-4).fired());
        assertEquals(0, Magazine.fire(-4).loaded());
    }

    @Test
    void fullDamageWithinEffectiveRangeThenFallsOff() {
        assertEquals(GUN.damage(), GUN.damageAt(1), 1e-6);
        assertEquals(GUN.damage(), GUN.damageAt(GUN.effectiveRange()), 1e-6);
        float mid = GUN.damageAt((GUN.effectiveRange() + GUN.maxRange()) / 2);
        assertTrue(mid < GUN.damage() && mid > GUN.damage() * 0.4F);
        assertEquals(GUN.damage() * 0.4F, GUN.damageAt(GUN.maxRange()), 1e-4);
        assertEquals(0F, GUN.damageAt(GUN.maxRange() + 0.01), 1e-6);
    }

    @Test
    void aGunshotIsHeardFurtherThanItReaches() {
        assertTrue(GUN.noiseRadius() > GUN.maxRange() / 2);
        assertTrue(Hearing.hears(GUN.noiseRadius() - 1, GUN.noiseRadius(), false));
        assertFalse(Hearing.hears(GUN.noiseRadius() + 1, GUN.noiseRadius(), false));
    }

    @Test
    void soundCarriesFurtherAtNight() {
        double d = GUN.noiseRadius() * 1.2;
        assertFalse(Hearing.hears(d, GUN.noiseRadius(), false));
        assertTrue(Hearing.hears(d, GUN.noiseRadius(), true));
    }
}
