package io.github.b1tr0n1n.signal.item;

import io.github.b1tr0n1n.signal.domain.FirearmSpec;
import io.github.b1tr0n1n.signal.domain.Magazine;
import io.github.b1tr0n1n.signal.registry.ModDataComponents;
import io.github.b1tr0n1n.signal.registry.ModItems;
import io.github.b1tr0n1n.signal.registry.ModSounds;
import io.github.b1tr0n1n.signal.survival.Noise;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A revolver. Use fires one loaded round (server-side hitscan with spread and range falloff); sneak + use
 * holds to reload one round at a time from the rounds you carry. Every shot is a noise the Drawn hear.
 * Rules live in {@link Magazine} and {@link FirearmSpec}; this class only applies them to the world.
 */
public final class RevolverItem extends Item {
    private static final int RELOAD_HOLD = 72000;
    private final FirearmSpec spec;

    public RevolverItem(FirearmSpec spec, Properties properties) {
        super(properties.component(ModDataComponents.LOADED_ROUNDS.get(), 0));
        this.spec = spec;
    }

    public FirearmSpec spec() {
        return spec;
    }

    public static int loaded(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LOADED_ROUNDS.get(), 0);
    }

    public static void setLoaded(ItemStack stack, int rounds) {
        stack.set(ModDataComponents.LOADED_ROUNDS.get(), rounds);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Sneak + use reloads while there is room and rounds to load; otherwise it fires like any use, so a
        // crouching player (the quiet stance) can always shoot.
        if (player.isShiftKeyDown() && Magazine.loadOne(loaded(stack), spec.capacity(), carried(player)).loadedOne()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            if (loaded(stack) > 0) {
                player.setXRot(player.getXRot() - 2.5F); // recoil: the muzzle climbs
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        fire((ServerLevel) level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Pull the trigger: a shot, or a dry click on an empty chamber. Server only. */
    public void fire(ServerLevel level, Player player, ItemStack stack) {
        io.github.b1tr0n1n.signal.survival.Reloading.cancel(player); // pulling the trigger stops a reload
        Magazine.Shot shot = Magazine.fire(loaded(stack));
        if (!shot.fired()) {
            level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.REVOLVER_DRY.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            player.getCooldowns().addCooldown(this, 6);
            return;
        }
        setLoaded(stack, shot.loaded());
        player.getCooldowns().addCooldown(this, spec.cooldownTicks());
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.REVOLVER_SHOT.get(), SoundSource.PLAYERS,
                5.0F, 0.95F + level.random.nextFloat() * 0.1F);

        Vec3 eye = player.getEyePosition();
        Vec3 dir = spread(player, spec.spreadDegrees(), level);
        Vec3 muzzle = eye.add(dir.scale(0.8));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.03, 0.03, 0.03, 0.01);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);

        Vec3 end = eye.add(dir.scale(spec.maxRange()));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, stop,
                new AABB(eye, stop).inflate(1.0), e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(),
                eye.distanceToSqr(stop));
        if (hit != null) {
            Entity target = hit.getEntity();
            double distance = eye.distanceTo(hit.getLocation());
            float damage = spec.damageAt(distance);
            if (target instanceof LivingEntity living && hit.getLocation().y > living.getEyeY() - 0.3) {
                damage *= 1.5F; // a head hit
            }
            target.hurt(level.damageSources().playerAttack(player), damage);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 3, 0.1, 0.1, 0.1, 0.1);
        } else if (block.getType() == HitResult.Type.BLOCK) {
            Vec3 p = block.getLocation();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(block.getBlockPos())),
                    p.x, p.y, p.z, 8, 0.05, 0.05, 0.05, 0.1);
        }
        Noise.emit(level, player.position(), spec.noiseRadius());
    }

    /** The aim, offset by a random yaw and pitch within the spread: the same cone whichever way you face. */
    private static Vec3 spread(Player player, double degrees, Level level) {
        float dy = (float) ((level.random.nextDouble() * 2 - 1) * degrees);
        float dp = (float) ((level.random.nextDouble() * 2 - 1) * degrees);
        return Vec3.directionFromRotation(player.getXRot() + dp, player.getYRot() + dy);
    }

    // ------------------------------------------------------------------ reloading

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return RELOAD_HOLD;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int held = RELOAD_HOLD - remaining;
        if (held == 0 || held % spec.reloadTicksPerRound() != 0) {
            return;
        }
        Magazine.Load load = loadOneRound(player, stack);
        if (!load.loadedOne() || load.loaded() >= spec.capacity() || carried(player) == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.REVOLVER_COCK.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            player.stopUsingItem();
        }
    }

    /** Move one carried round into the gun if there is room and a round; shared by both ways of reloading. */
    public Magazine.Load loadOneRound(Player player, ItemStack stack) {
        Magazine.Load load = Magazine.loadOne(loaded(stack), spec.capacity(), carried(player));
        if (load.loadedOne()) {
            if (!player.getAbilities().instabuild) {
                takeOneRound(player);
            }
            setLoaded(stack, load.loaded());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.REVOLVER_LOAD.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        return load;
    }

    /** Rounds of this gun's calibre the player carries (creative counts as plenty). */
    public static int carried(Player player) {
        if (player.getAbilities().instabuild) {
            return 99;
        }
        int n = 0;
        for (ItemStack s : pockets(player)) {
            if (s.is(ModItems.REVOLVER_ROUND.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void takeOneRound(Player player) {
        for (ItemStack s : pockets(player)) {
            if (s.is(ModItems.REVOLVER_ROUND.get())) {
                s.shrink(1);
                return;
            }
        }
    }

    /** Main inventory and the offhand slot: everywhere a round can be carried. */
    private static Iterable<ItemStack> pockets(Player player) {
        java.util.List<ItemStack> all = new java.util.ArrayList<>(player.getInventory().items);
        all.addAll(player.getInventory().offhand);
        return all;
    }

    // ------------------------------------------------------------------ display

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(loaded(stack), 0, spec.capacity()) / spec.capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE89E34;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.signal.revolver.rounds", Mth.clamp(loaded(stack), 0, spec.capacity()), spec.capacity())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.signal.revolver.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
