package io.github.b1tr0n1n.signal.entity;

import io.github.b1tr0n1n.signal.domain.DrawnBudget;
import io.github.b1tr0n1n.signal.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Drawn: tall, eyeless things that were once animals and people. They do not see; they hear. A player
 * is found by the noise they make (sprinting carries, crouching barely does), a gunshot by everything for a
 * long way, and soon a radio transmission. Original model and sounds.
 */
public class DrawnEntity extends Monster {
    /** Where the last noise came from, and for how many more ticks it is worth investigating. */
    @Nullable
    private Vec3 heard;
    private int heardTicks;
    private int alertCooldown;

    public DrawnEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new WindUpAttackGoal(this, 1.3));
        goalSelector.addGoal(3, new InvestigateNoiseGoal(this, 1.15));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new HearPlayerGoal(this));
    }

    /** Something loud happened at {@code source}. */
    public void hear(Vec3 source) {
        heard = source;
        heardTicks = 600;
        if (alertCooldown <= 0 && getTarget() == null) {
            playSound(ModSounds.DRAWN_ALERT.get(), 2.0F, 0.9F + random.nextFloat() * 0.2F);
            alertCooldown = 100;
        }
    }

    @Nullable
    public Vec3 heard() {
        return heardTicks > 0 ? heard : null;
    }

    public void forgetHeard() {
        heard = null;
        heardTicks = 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (heardTicks > 0) {
            heardTicks--;
        }
        if (alertCooldown > 0) {
            alertCooldown--;
        }
        if (!level().isClientSide && tickCount % 20 == 0 && !isPersistenceRequired()) {
            Player nearest = level().getNearestPlayer(this, -1.0);
            double d = nearest == null ? Double.MAX_VALUE : distanceTo(nearest);
            if (DrawnBudget.retreatsAtDawn(level().isDay(), getTarget() != null, d)) {
                discard(); // back into the trees, unseen
            }
        }
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DRAWN_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DRAWN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DRAWN_DEATH.get();
    }
}
