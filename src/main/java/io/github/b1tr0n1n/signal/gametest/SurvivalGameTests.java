package io.github.b1tr0n1n.signal.gametest;

import com.mojang.authlib.GameProfile;
import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.domain.FirearmSpec;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import io.github.b1tr0n1n.signal.entity.HearPlayerGoal;
import io.github.b1tr0n1n.signal.item.RevolverItem;
import io.github.b1tr0n1n.signal.registry.ModEntities;
import io.github.b1tr0n1n.signal.registry.ModItems;
import io.github.b1tr0n1n.signal.survival.Noise;
import io.github.b1tr0n1n.signal.survival.Reloading;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Phase A survival: the revolver, its rounds, and the Drawn hearing it. */
@GameTestHolder(SignalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurvivalGameTests {
    private static final FirearmSpec GUN = FirearmSpec.REVOLVER;

    private SurvivalGameTests() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** A test shooter standing at {@code rel}, facing south (+z), level. */
    private static ServerPlayer shooter(GameTestHelper helper, BlockPos rel, String name) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(rel));
        p.moveTo(at.x, at.y, at.z, 0F, 0F);
        p.getInventory().clearContent();
        p.setPose(Pose.STANDING);
        p.setSprinting(false);
        return p;
    }

    /** A Drawn that stands still where it is put (no AI), so the test is deterministic. */
    private static DrawnEntity drawn(GameTestHelper helper, BlockPos rel) {
        ServerLevel level = helper.getLevel();
        DrawnEntity d = ModEntities.DRAWN.get().create(level);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(rel));
        d.moveTo(at.x, at.y, at.z, 180F, 0F);
        d.setNoAi(true);
        level.addFreshEntity(d);
        return d;
    }

    private static ItemStack revolver(int loaded) {
        ItemStack gun = new ItemStack(ModItems.REVOLVER.get());
        RevolverItem.setLoaded(gun, loaded);
        return gun;
    }

    @GameTest(template = "empty_5")
    public static void revolverShotSpendsOneRoundAndWounds(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_a");
        DrawnEntity target = drawn(helper, new BlockPos(2, 1, 4));
        ItemStack gun = revolver(GUN.capacity());
        float before = target.getHealth();
        ModItems.REVOLVER.get().fire(helper.getLevel(), p, gun);
        check(RevolverItem.loaded(gun) == GUN.capacity() - 1, "a shot must spend exactly one round, have " + RevolverItem.loaded(gun));
        check(target.getHealth() < before, "a shot at 4 blocks must wound the Drawn (" + before + " -> " + target.getHealth() + ")");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void emptyRevolverOnlyClicks(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_b");
        DrawnEntity target = drawn(helper, new BlockPos(2, 1, 4));
        ItemStack gun = revolver(0);
        float before = target.getHealth();
        ModItems.REVOLVER.get().fire(helper.getLevel(), p, gun);
        check(RevolverItem.loaded(gun) == 0, "an empty gun stays empty");
        check(target.getHealth() == before, "an empty gun must not hurt anything");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void gunshotIsHeardByTheDrawn(GameTestHelper helper) {
        DrawnEntity near = drawn(helper, new BlockPos(2, 1, 4));
        check(near.heard() == null, "nothing heard yet");
        int heard = Noise.emit(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 0))), GUN.noiseRadius());
        check(heard >= 1 && near.heard() != null, "a Drawn 4 blocks away must hear a gunshot");
        int quiet = Noise.emit(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 1, 0))).add(0, 0, 400), GUN.noiseRadius());
        check(quiet == 0, "a shot 400 blocks away must not be heard here");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void crouchingWithAFullGunFires(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_e");
        p.getInventory().add(new ItemStack(ModItems.REVOLVER_ROUND.get(), 3));
        ItemStack gun = revolver(GUN.capacity());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
        p.setShiftKeyDown(true);
        ModItems.REVOLVER.get().use(helper.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        check(RevolverItem.loaded(p.getMainHandItem()) == GUN.capacity() - 1,
                "sneak + use with a full gun must fire, have " + RevolverItem.loaded(p.getMainHandItem()));
        check(!p.isUsingItem(), "and must not start reloading");
        p.setShiftKeyDown(false);
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void reloadingTakesRoundsFromThePockets(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_c");
        p.getInventory().add(new ItemStack(ModItems.REVOLVER_ROUND.get(), 3));
        ItemStack gun = revolver(4);
        ModItems.REVOLVER.get().onUseTick(helper.getLevel(), p, gun, 72000 - GUN.reloadTicksPerRound());
        check(RevolverItem.loaded(gun) == 5, "one reload step loads one round, have " + RevolverItem.loaded(gun));
        check(RevolverItem.carried(p) == 2, "and takes it from the pockets, carried " + RevolverItem.carried(p));
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void reloadKeyLoadsUntilThePocketsAreEmpty(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_f");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, revolver(2)); // gun first: it takes the held slot
        p.getInventory().add(new ItemStack(ModItems.REVOLVER_ROUND.get(), 3));
        check(Reloading.start(p), "reload must start with room and rounds");
        for (int i = 0; i < GUN.reloadTicksPerRound() * 6; i++) {
            Reloading.tick(p);
        }
        check(RevolverItem.loaded(p.getMainHandItem()) == 5, "2 loaded + 3 carried = 5, have " + RevolverItem.loaded(p.getMainHandItem()));
        check(RevolverItem.carried(p) == 0, "every carried round loaded");
        check(!Reloading.isReloading(p), "reload ends when the pockets are empty");
        check(!Reloading.start(p) || RevolverItem.carried(p) > 0, "nothing to load: reload must not start");
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void firingCancelsAReload(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_g");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, revolver(1)); // gun first: it takes the held slot
        p.getInventory().add(new ItemStack(ModItems.REVOLVER_ROUND.get(), 5));
        check(Reloading.start(p), "reload starts");
        ModItems.REVOLVER.get().fire(helper.getLevel(), p, p.getMainHandItem());
        check(!Reloading.isReloading(p), "pulling the trigger stops the reload");
        Reloading.cancel(p);
        helper.succeed();
    }

    @GameTest(template = "empty_5")
    public static void theDrawnHearSprintingFurtherThanCrouching(GameTestHelper helper) {
        ServerPlayer p = shooter(helper, new BlockPos(2, 1, 0), "signal_shooter_d");
        double walk = HearPlayerGoal.audibleAt(p);
        p.setSprinting(true);
        double sprint = HearPlayerGoal.audibleAt(p);
        p.setSprinting(false);
        p.setPose(Pose.CROUCHING);
        double crouch = HearPlayerGoal.audibleAt(p);
        check(crouch < walk && walk < sprint, "hearing must rank crouch < walk < sprint: " + crouch + " " + walk + " " + sprint);
        helper.succeed();
    }
}
