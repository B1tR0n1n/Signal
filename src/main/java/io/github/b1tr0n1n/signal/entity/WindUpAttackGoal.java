package io.github.b1tr0n1n.signal.entity;

import io.github.b1tr0n1n.signal.registry.ModSounds;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee with a tell: when a Drawn closes on its prey it rattles first and does not strike for a full
 * second, so a careful player who hears it has a moment to answer. (Playtest 2026-10-11: killed from full
 * health in three seconds by something never seen or heard.)
 */
public class WindUpAttackGoal extends MeleeAttackGoal {
    private final DrawnEntity drawn;

    public WindUpAttackGoal(DrawnEntity drawn, double speed) {
        super(drawn, speed, false);
        this.drawn = drawn;
    }

    @Override
    public void start() {
        super.start();
        resetAttackCooldown();
        drawn.playSound(ModSounds.DRAWN_ALERT.get(), 2.5F, 0.6F + drawn.getRandom().nextFloat() * 0.1F);
    }

    /** Ticks until the next strike may land (read by the GameTests). */
    public int ticksToStrike() {
        return getTicksUntilNextAttack();
    }
}
