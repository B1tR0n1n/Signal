package io.github.b1tr0n1n.signal.survival;

import io.github.b1tr0n1n.signal.domain.Hearing;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Loud things the Drawn can hear: gunshots now, radio transmissions next. Whether a noise reaches a listener
 * is {@link Hearing}'s rule; this only finds the listeners and tells them where it came from.
 */
public final class Noise {
    private Noise() {
    }

    /** @return how many Drawn heard it */
    public static int emit(ServerLevel level, Vec3 source, int radius) {
        boolean night = level.isNight();
        double reach = radius * (night ? Hearing.NIGHT_FACTOR : 1.0);
        int heard = 0;
        for (DrawnEntity d : level.getEntitiesOfClass(DrawnEntity.class, new AABB(source, source).inflate(reach))) {
            if (Hearing.hears(d.position().distanceTo(source), radius, night)) {
                d.hear(source);
                heard++;
            }
        }
        return heard;
    }
}
