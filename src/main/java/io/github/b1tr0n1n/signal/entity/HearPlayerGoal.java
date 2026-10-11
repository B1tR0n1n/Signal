package io.github.b1tr0n1n.signal.entity;

import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * The Drawn find players by ear, not by sight: a sprinting player is heard at 16 blocks, a walking one at 8,
 * a crouching one only at 3. Walls do not matter; noise does.
 */
public final class HearPlayerGoal extends TargetGoal {
    static final double SPRINT = 16, WALK = 8, CROUCH = 3;
    private final DrawnEntity drawn;
    private Player found;
    /** Own countdown: Mob only re-checks idle goals every other tick (by tickCount + id), so a global
     *  {@code tickCount % n} gate would never open for half of all Drawn. */
    private int nextScan;

    HearPlayerGoal(DrawnEntity drawn) {
        super(drawn, false, false);
        this.drawn = drawn;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    public static double audibleAt(Player p) {
        return p.isCrouching() ? CROUCH : p.isSprinting() ? SPRINT : WALK;
    }

    @Override
    public boolean canUse() {
        if (--nextScan > 0) {
            return false;
        }
        nextScan = 5; // canUse runs every second tick: a scan about every 10 ticks
        found = null;
        double best = Double.MAX_VALUE;
        for (Player p : drawn.level().players()) {
            if (p.isSpectator() || p.isCreative() || !p.isAlive()) {
                continue;
            }
            double d = p.distanceTo(drawn);
            if (d <= audibleAt(p) && d < best) {
                best = d;
                found = p;
            }
        }
        return found != null;
    }

    @Override
    public void start() {
        drawn.setTarget(found);
        super.start();
    }
}
