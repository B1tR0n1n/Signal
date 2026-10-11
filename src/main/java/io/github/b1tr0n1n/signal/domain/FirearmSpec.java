package io.github.b1tr0n1n.signal.domain;

/**
 * A firearm's fixed characteristics. Pure data: the Minecraft side reads these to fire, reload and make
 * noise. Damage is in half-hearts (Minecraft health points); ranges and noise in blocks.
 *
 * @param capacity            rounds the gun holds
 * @param damage              damage per hit within {@code effectiveRange}
 * @param effectiveRange      full damage up to here
 * @param maxRange            beyond this a shot hits nothing; damage falls linearly to 40 % on the way
 * @param spreadDegrees       half-angle of the random cone a shot leaves in
 * @param cooldownTicks       minimum ticks between shots
 * @param reloadTicksPerRound ticks to load one round
 * @param noiseRadius         how far the shot is heard by day (further at night, see {@link Hearing})
 */
public record FirearmSpec(String id, int capacity, float damage, double effectiveRange, double maxRange,
                          double spreadDegrees, int cooldownTicks, int reloadTicksPerRound, int noiseRadius) {

    /** Elias's service revolver: six rounds, slow to load, loud. */
    public static final FirearmSpec REVOLVER = new FirearmSpec("revolver", 6, 9.0F, 24, 64, 1.2, 12, 14, 64);

    public FirearmSpec {
        if (capacity <= 0 || damage <= 0 || effectiveRange <= 0 || maxRange < effectiveRange || noiseRadius <= 0) {
            throw new IllegalArgumentException("invalid firearm spec " + id);
        }
    }

    /** Damage of a hit at {@code distance}: full, then a linear fall to 40 % at max range, nothing beyond. */
    public float damageAt(double distance) {
        if (distance > maxRange) {
            return 0F;
        }
        if (distance <= effectiveRange) {
            return damage;
        }
        double t = (distance - effectiveRange) / (maxRange - effectiveRange);
        return (float) (damage * (1.0 - 0.6 * t));
    }
}
