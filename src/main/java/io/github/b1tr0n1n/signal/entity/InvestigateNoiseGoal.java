package io.github.b1tr0n1n.signal.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Walk to where the last noise came from, then listen there a moment. Yields to a real target. */
final class InvestigateNoiseGoal extends Goal {
    private final DrawnEntity drawn;
    private final double speed;
    private Vec3 goal;
    private int listen;

    InvestigateNoiseGoal(DrawnEntity drawn, double speed) {
        this.drawn = drawn;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return drawn.getTarget() == null && drawn.heard() != null;
    }

    @Override
    public void start() {
        goal = drawn.heard();
        listen = 0;
        drawn.getNavigation().moveTo(goal.x, goal.y, goal.z, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return drawn.getTarget() == null && goal != null && listen < 60;
    }

    @Override
    public void tick() {
        Vec3 latest = drawn.heard();
        if (latest != null && latest.distanceToSqr(goal) > 4) {
            goal = latest; // a newer noise: turn toward it
            drawn.getNavigation().moveTo(goal.x, goal.y, goal.z, speed);
        }
        if (drawn.position().distanceToSqr(goal) < 9 || drawn.getNavigation().isDone()) {
            listen++;
            drawn.getLookControl().setLookAt(goal.x, goal.y + 1, goal.z);
        }
    }

    @Override
    public void stop() {
        if (listen >= 60) {
            drawn.forgetHeard();
        }
        goal = null;
        drawn.getNavigation().stop();
    }
}
