package io.github.b1tr0n1n.signal.gametest;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.block.DistributionPanelBlock;
import io.github.b1tr0n1n.signal.block.GeneratorBlock;
import io.github.b1tr0n1n.signal.block.PumpControllerBlock;
import io.github.b1tr0n1n.signal.block.RelayControllerBlock;
import io.github.b1tr0n1n.signal.block.RelayState;
import io.github.b1tr0n1n.signal.block.ServiceLampBlock;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import io.github.b1tr0n1n.signal.campaign.SignalCampaignData;
import io.github.b1tr0n1n.signal.campaign.SiteInteractions;
import io.github.b1tr0n1n.signal.campaign.SiteProjector;
import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.domain.FuseState;
import io.github.b1tr0n1n.signal.domain.RadioCatalog;
import io.github.b1tr0n1n.signal.domain.Service;
import io.github.b1tr0n1n.signal.registry.ModBlocks;
import io.github.b1tr0n1n.signal.registry.ModItems;
import io.github.b1tr0n1n.signal.scenario.BellwetherScenario;
import io.github.b1tr0n1n.signal.scenario.ScenarioGuard;
import io.github.b1tr0n1n.signal.scenario.ScenarioService;
import io.github.b1tr0n1n.signal.scenario.ServiceFacility;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

/**
 * Integration tests run by {@code ./gradlew runGameTestServer} on a dedicated-server-type environment,
 * which also proves common code does not load client-only classes.
 */
@GameTestHolder(SignalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SignalGameTests {
    private SignalGameTests() {
    }

    /** Highest non-air block y (relative) in a test column, scanning down from the volume's top. */
    private static int topAt(GameTestHelper helper, int x, int z) {
        for (int y = 40; y > 0; y--) {
            if (!helper.getBlockState(new BlockPos(x, y, z)).isAir() && !helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.SPRUCE_LOG)
                    && !helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.SPRUCE_LEAVES)) {
                return y;
            }
        }
        return 1;
    }

    private static boolean touches(ServerLevel level, BlockPos p, java.util.function.Predicate<BlockState> joins) {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            if (joins.test(level.getBlockState(p.relative(d)))) {
                return true;
            }
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static BlockPos find(SignalCampaignData data, String node, Class<?> blockClass, ServerLevel level) {
        for (Map.Entry<BlockPos, String> e : data.bindings().entrySet()) {
            if (e.getValue().equals(node) && blockClass.isInstance(level.getBlockState(e.getKey()).getBlock())) {
                return e.getKey();
            }
        }
        throw new GameTestAssertException("No bound " + blockClass.getSimpleName() + " for " + node);
    }

    private static int countFuses(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.REPLACEMENT_FUSE.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean lampLit(ServerLevel level, SignalCampaignData data, String node) {
        boolean any = false;
        for (Map.Entry<BlockPos, String> e : data.bindings().entrySet()) {
            BlockState s = level.getBlockState(e.getKey());
            if (e.getValue().equals(node) && s.getBlock() instanceof ServiceLampBlock) {
                if (!s.getValue(ServiceLampBlock.LIT)) {
                    return false;
                }
                any = true;
            }
        }
        check(any, "no lamp bound to " + node);
        return true;
    }

    /**
     * The milestone-one loop on the real generated site: inspect, refused isolator, fuse repair consuming
     * exactly one fuse, supply restored, valid allocation, rejected over-capacity and stale applies,
     * world indicators, pump gauge and header tank, one radio acknowledgement, and a save/load round trip.
     */
    @GameTest(template = "bellwether_site", timeoutTicks = 1200)
    public static void bellwetherRepairLoop(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        if (data.isInitialized()) {
            data.abandonForGameTest();
        }
        // Synthetic terrain: rolling ground at rel y 7 with a hill north-east where the tower ridge should be.
        for (int x = 0; x < 176; x++) {
            for (int z = 0; z < 144; z++) {
                double hill = Math.max(0, 12 - 0.9 * Math.hypot(x - 156, z - 26));
                int top = 7 + (int) Math.round(hill + 1.5 * Math.sin(x / 9.0) * Math.cos(z / 11.0));
                for (int y = 1; y <= top; y++) {
                    helper.setBlock(new BlockPos(x, y, z), y == top ? Blocks.GRASS_BLOCK : y > top - 3 ? Blocks.DIRT : Blocks.STONE);
                }
                // A grid of tall trunks, so cuts through the woods are exercised (no crown may be left floating).
                if (x % 11 == 5 && z % 11 == 5) {
                    for (int y = top + 1; y <= top + 12; y++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.SPRUCE_LOG);
                    }
                    helper.setBlock(new BlockPos(x, top + 13, z), Blocks.SPRUCE_LEAVES);
                }
            }
        }
        // Ponds scattered over the site, so routes and pads meet water (none may be left as a hanging wall).
        for (int px = 8; px < 170; px += 26) {
            for (int pz = 6; pz < 138; pz += 22) {
                int m = Integer.MAX_VALUE;
                for (int x = px; x < px + 6; x++) {
                    for (int z = pz; z < pz + 6; z++) {
                        m = Math.min(m, topAt(helper, x, z));
                    }
                }
                if (m < 8) {
                    continue; // keep the lowest ground dry: the site's depth margin must stay above the test floor
                }
                for (int x = px; x < px + 6; x++) {
                    for (int z = pz; z < pz + 6; z++) {
                        for (int y = m - 1; y <= m + 14; y++) {
                            helper.setBlock(new BlockPos(x, y, z), y == m - 1 ? Blocks.WATER : Blocks.AIR);
                        }
                    }
                }
            }
        }
        // A mound with a pond on top, across the road's way in: grading the road through the mound must not
        // leave the pond hanging as a wall of water.
        int crest = 0;
        for (int x = 36; x <= 56; x++) {
            for (int z = 62; z <= 82; z++) {
                crest = Math.max(crest, topAt(helper, x, z));
            }
        }
        crest += 4;
        for (int x = 36; x <= 56; x++) {
            for (int z = 62; z <= 82; z++) {
                int d = Math.max(Math.abs(x - 46), Math.abs(z - 72));
                int base = topAt(helper, x, z);
                int top = d <= 5 ? crest : base + Math.round((crest - base) * (10 - d) / 5.0f);
                for (int y = base + 1; y <= top + 14; y++) {
                    BlockState fill = y > top ? Blocks.AIR.defaultBlockState()
                            : y == top ? (d <= 3 ? Blocks.WATER : Blocks.GRASS_BLOCK).defaultBlockState() : Blocks.DIRT.defaultBlockState();
                    helper.setBlock(new BlockPos(x, y, z), fill);
                }
            }
        }
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 8, 72));
        // Forge's FakePlayer: a real ServerPlayer with a no-op network handler. (The vanilla mock player
        // cannot be placed on a Forge-family server: login hooks need a Netty pipeline.)
        ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("5167a1e1-0000-4000-8000-000000000001"), "signal_test"));

        boolean spawningBefore = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING);
        ScenarioService.CreateResult created = ScenarioService.create(level, anchor, player.getUUID());
        check(created.status() == ScenarioService.Status.CREATED, "scenario not created: " + created.status() + " " + created.problemKey()
                + (created.guard() != null ? " occupied=" + created.guard().occupied() + " first=" + created.guard().first() : "")
                + " bounds=" + created.bounds());
        check(ScenarioService.create(level, anchor, player.getUUID()).status() == ScenarioService.Status.ALREADY_EXISTS,
                "second create must refuse");
        ScenarioService.begin(level, data);
        check(helper.absolutePos(new BlockPos(0, 0, 0)).getY() < data.bounds().minY(), "bounds must stay above the test floor");
        // The site keeps hostiles out without touching the world's spawning: wildlife may still spawn in it.
        check(level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING) == spawningBefore,
                "creating the site must not change the world's mob spawning rule");
        BlockPos yard = data.frame().above();
        net.minecraft.world.entity.monster.Zombie zombie = new net.minecraft.world.entity.monster.Zombie(level);
        zombie.moveTo(yard.getX() + 0.5, yard.getY(), yard.getZ() + 0.5);
        net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck hostile = new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck(
                zombie, level, net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(hostile);
        check(hostile.getResult() == net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL,
                "a natural zombie spawn inside the site must be refused");
        net.minecraft.world.entity.animal.Rabbit rabbit = new net.minecraft.world.entity.animal.Rabbit(net.minecraft.world.entity.EntityType.RABBIT, level);
        rabbit.moveTo(yard.getX() + 0.5, yard.getY(), yard.getZ() + 0.5);
        net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck wild = new net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck(
                rabbit, level, net.minecraft.world.entity.MobSpawnType.NATURAL, null);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(wild);
        check(wild.getResult() != net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL,
                "wildlife must not be refused inside the site");
        for (BlockPos p : data.bindings().keySet()) {
            check(data.bounds().isInside(p), "binding outside bounds: " + p);
        }

        BlockPos gen = find(data, BellwetherSite.NODE_GENERATOR, GeneratorBlock.class, level);
        BlockPos panel = find(data, BellwetherSite.NODE_PANEL, DistributionPanelBlock.class, level);
        BlockPos pump = find(data, BellwetherSite.NODE_PUMP, PumpControllerBlock.class, level);
        BlockPos relay = find(data, BellwetherSite.NODE_RELAY, RelayControllerBlock.class, level);
        BlockPos cabinet = data.frame().offset(ServiceFacility.CABINET);
        BlockPos tank = data.bindings().entrySet().stream().filter(e -> e.getValue().equals(SiteProjector.NODE_PUMP_TANK))
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> new GameTestAssertException("no header tank"));
        BellwetherBus bus = data.site().bus();

        // The tower: beacon bound to the relay, and a climbable ladder on a solid riser through both platforms.
        BlockPos beacon = data.bindings().entrySet().stream()
                .filter(e -> e.getValue().equals(BellwetherSite.NODE_RELAY) && level.getBlockState(e.getKey()).getBlock() instanceof ServiceLampBlock)
                .map(Map.Entry::getKey).max(java.util.Comparator.comparingInt(BlockPos::getY))
                .orElseThrow(() -> new GameTestAssertException("no tower beacon"));
        BlockPos towerBase = beacon.below(38);
        check(towerBase.getY() > data.frame().getY(), "tower should stand on the ridge above the compound");
        for (int y = 0; y <= 28; y++) {
            check(level.getBlockState(towerBase.offset(0, y, 1)).is(Blocks.LADDER), "ladder missing at +" + y);
            check(level.getBlockState(towerBase.offset(0, y, 0)).isSolid(), "ladder riser not solid at +" + y);
        }
        // No floating pieces: every tower bar joins a neighbour, and every cable link touches another link or
        // a pole (chains and bars only render joined side by side or stacked, never corner to corner).
        int chains = 0;
        for (BlockPos p : BlockPos.betweenClosed(towerBase.offset(-3, 1, -3), towerBase.offset(3, 27, 3))) {
            if (level.getBlockState(p).is(Blocks.IRON_BARS)) {
                check(touches(level, p, s -> s.is(Blocks.IRON_BARS) || s.is(BlockTags.WALLS) || s.isSolid()), "loose tower bar at " + p);
            }
        }
        BoundingBox b = data.bounds();
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            if (level.getBlockState(p).is(Blocks.CHAIN)) {
                chains++;
                check(touches(level, p, s -> s.is(Blocks.CHAIN) || s.is(BlockTags.LOGS)), "loose cable link at " + p);
            }
        }
        check(chains > 20, "power line cable missing (" + chains + " links)");
        // No hanging water: every fluid inside the site is held from the sides and from below.
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            if (!level.getFluidState(p).isEmpty()) {
                for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH,
                        net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST,
                        net.minecraft.core.Direction.DOWN}) {
                    BlockPos n = p.relative(d);
                    check(!b.isInside(n) || !level.getBlockState(n).isAir(), "water hangs over air at " + p + " (" + d + ")");
                }
            }
        }
        // No floating trunks: a cut through a tree must take the whole tree above it.
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            if (level.getBlockState(p).is(Blocks.SPRUCE_LOG)) {
                check(!level.getBlockState(p.below()).isAir(), "floating tree trunk at " + p);
            }
        }

        // Bellwether: two lived-in houses, each front door on its footing with walkable lane in front of it, and
        // no footing (house or facility stone bricks) left standing on air.
        int doors = 0;
        for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SPRUCE_DOOR) && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                doors++;
                BlockPos step = p.relative(s.getValue(DoorBlock.FACING));
                check(level.getBlockState(p.below()).is(Blocks.STONE_BRICKS), "house door not on its footing at " + p);
                check(!level.getBlockState(step).isSolid() && !level.getBlockState(step.above()).isSolid(),
                        "house door blocked at " + p);
                check(level.getBlockState(step.below()).isSolid() || level.getBlockState(step).getBlock() instanceof SlabBlock,
                        "no ground in front of the house door at " + p);
            } else if (s.is(Blocks.STONE_BRICKS)) {
                check(!level.getBlockState(p.below()).isAir(), "footing over air at " + p);
            }
        }
        check(doors == 2, "expected the two lived-in houses' doors, found " + doors);

        check(bus.fuse() == FuseState.DAMAGED && bus.isolated(), "fresh scenario must be faulted and isolated");
        check(level.getBlockState(panel).getValue(DistributionPanelBlock.FAULT), "panel should show fault");
        check(!level.getBlockState(gen).getValue(GeneratorBlock.RUNNING), "generator should not be supplying");
        check(data.site().radio().isDelivered(RadioCatalog.OPENING.id()), "opening should be delivered");

        // Take the spares from the marked cabinet.
        check(level.getBlockEntity(cabinet) instanceof Container, "fuse cabinet missing");
        Container c = (Container) level.getBlockEntity(cabinet);
        int inCabinet = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(ModItems.REPLACEMENT_FUSE.get())) {
                inCabinet += c.getItem(i).getCount();
                player.getInventory().add(c.removeItemNoUpdate(i));
            }
        }
        check(inCabinet == 2, "cabinet should hold 2 spare fuses, found " + inCabinet);
        player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
        player.moveTo(panel.getX() + 0.5, panel.getY() - 1, panel.getZ() + 2.5, 180.0F, 0.0F);

        // A machine placed outside the bound equipment does nothing.
        BlockPos stray = helper.absolutePos(new BlockPos(1, 60, 1));
        level.setBlockAndUpdate(stray, ModBlocks.GENERATOR.get().defaultBlockState());
        int before = bus.revision();
        SiteInteractions.onUse(level, player, stray, level.getBlockState(stray));
        check(bus.revision() == before, "unbound generator must not change campaign state");
        level.setBlockAndUpdate(stray, Blocks.AIR.defaultBlockState());

        int rev0 = bus.revision();
        helper.startSequence()
                // Breakers may be set while isolated; nothing runs yet.
                .thenExecute(() -> {
                    check(SiteInteractions.applyAllocation(player, panel, EnumSet.of(Service.PUMP, Service.LOCAL_TRANSMITTER), rev0)
                            == SiteInteractions.PanelOutcome.DONE, "apply rejected by validation");
                    check(bus.configured().equals(EnumSet.of(Service.PUMP, Service.LOCAL_TRANSMITTER)), "breakers not set");
                    check(!bus.isRunning(Service.PUMP), "pump must not run while isolated");
                })
                // Isolator refuses to close onto the damaged fuse.
                .thenExecuteAfter(5, () -> {
                    SiteInteractions.onUse(level, player, gen, level.getBlockState(gen));
                    check(bus.isolated(), "isolator closed onto a damaged fuse");
                })
                // Valid repair consumes exactly one fuse.
                .thenExecuteAfter(5, () -> {
                    int rev = bus.revision();
                    SiteInteractions.installFuse(player, panel, rev);
                    check(bus.fuse() == FuseState.INSTALLED, "fuse not installed");
                    check(countFuses(player) == 1, "expected 1 fuse left, have " + countFuses(player));
                })
                // Duplicate submit (old revision) and a fresh second attempt consume nothing.
                .thenExecuteAfter(5, () -> {
                    SiteInteractions.installFuse(player, panel, rev0 + 1);
                })
                .thenExecuteAfter(5, () -> {
                    SiteInteractions.installFuse(player, panel, bus.revision());
                    check(countFuses(player) == 1, "repeated repair consumed a fuse: " + countFuses(player));
                    check(!level.getBlockState(panel).getValue(DistributionPanelBlock.FAULT), "panel still shows fault");
                })
                // Close the isolator: services start, indicators and the acknowledgement follow.
                .thenExecuteAfter(5, () -> {
                    SiteInteractions.onUse(level, player, gen, level.getBlockState(gen));
                    check(bus.busLive(), "bus not live after closing isolator");
                    check(level.getBlockState(gen).getValue(GeneratorBlock.RUNNING), "generator block not running");
                    check(lampLit(level, data, BellwetherSite.NODE_PUMP), "pump lamp(s) not lit");
                    check(lampLit(level, data, BellwetherSite.NODE_LOCAL_TX), "local transmitter lamp not lit");
                    check(level.getBlockState(relay).getValue(RelayControllerBlock.RELAY) == RelayState.OFF, "relay should be off");
                    check(data.site().radio().isDelivered(RadioCatalog.LOCAL_RESTORED.id()), "acknowledgement not delivered");
                })
                // Pump + relay = 8 on 6 units: refused, nothing changes.
                .thenExecuteAfter(5, () -> {
                    int rev = bus.revision();
                    SiteInteractions.applyAllocation(player, panel, EnumSet.of(Service.PUMP, Service.REGIONAL_RELAY), rev);
                    check(bus.revision() == rev, "over-capacity apply changed state");
                    check(bus.configured().equals(EnumSet.of(Service.PUMP, Service.LOCAL_TRANSMITTER)), "configuration changed");
                })
                // A stale panel does not overwrite the newer configuration.
                .thenExecuteAfter(5, () -> {
                    SiteInteractions.applyAllocation(player, panel, EnumSet.of(Service.REGIONAL_RELAY), rev0);
                    check(bus.configured().equals(EnumSet.of(Service.PUMP, Service.LOCAL_TRANSMITTER)), "stale apply overwrote configuration");
                })
                // Pressure climbs to full; gauge and header tank agree with it.
                .thenWaitUntil(() -> {
                    check(data.pumpPressure() == 3, "pressure " + data.pumpPressure());
                    check(level.getBlockState(pump).getValue(PumpControllerBlock.PRESSURE) == 3, "gauge not at 3");
                    BlockState tankState = level.getBlockState(tank);
                    check(tankState.is(Blocks.WATER_CAULDRON) && tankState.getValue(LayeredCauldronBlock.LEVEL) == 3, "header tank not full: " + tankState);
                })
                // Saved state round-trips: repair, allocation, messages and history survive.
                .thenExecute(() -> {
                    CompoundTag saved = data.save(new CompoundTag());
                    SignalCampaignData reloaded = SignalCampaignData.load(saved);
                    BellwetherBus rb = reloaded.site().bus();
                    check(reloaded.isActive(), "reloaded campaign inactive: " + reloaded.loadError());
                    check(rb.fuse() == FuseState.INSTALLED && rb.busLive(), "repair/supply lost on reload");
                    check(rb.configured().equals(bus.configured()) && rb.revision() == bus.revision(), "allocation lost on reload");
                    check(reloaded.site().radio().transcript().equals(data.site().radio().transcript()), "transcript changed on reload");
                    check(reloaded.site().radio().unpresented().equals(data.site().radio().unpresented()), "presentation state changed");
                    check(reloaded.site().history().events().equals(data.site().history().events()), "history changed on reload");
                    check(reloaded.bindings().equals(data.bindings()), "bindings lost on reload");
                    check(reloaded.site().evaluateTriggers().isEmpty(), "reload re-fired a one-shot consequence");
                    // Replaying the transcript adds nothing.
                    int events = data.site().history().events().size();
                    io.github.b1tr0n1n.signal.campaign.RadioPresenter.showTranscript(player);
                    check(data.site().history().events().size() == events, "transcript replay recorded an event");
                })
                .thenSucceed();
    }

    /** Scenario creation refuses occupied terrain and accepts natural terrain. */
    @GameTest(template = "empty_5")
    public static void guardRefusesOccupiedArea(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos min = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos max = helper.absolutePos(new BlockPos(4, 4, 4));
        BoundingBox box = BoundingBox.fromCorners(min, max);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.OAK_LOG);
        check(ScenarioGuard.check(level, box).ok(), "natural terrain must be accepted");
        // Sand or gravel falling as its chunk generates is terrain in motion, not somebody's build.
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.GRAVEL);
        net.minecraft.world.entity.item.FallingBlockEntity falling = net.minecraft.world.entity.item.FallingBlockEntity.fall(
                level, helper.absolutePos(new BlockPos(1, 3, 1)), Blocks.GRAVEL.defaultBlockState());
        check(ScenarioGuard.check(level, box).ok(), "a falling gravel block must not block construction");
        falling.discard();
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.CHEST);
        ScenarioGuard.Report r = ScenarioGuard.check(level, box);
        check(!r.ok() && r.occupied() >= 1, "chest must block construction");
        // A dungeon's spawner and unopened loot chest are world generation, not player work.
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.SPAWNER);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.CHEST);
        if (level.getBlockEntity(helper.absolutePos(new BlockPos(3, 2, 2))) instanceof ChestBlockEntity loot) {
            loot.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 7L);
        }
        check(ScenarioGuard.check(level, box).ok(), "dungeon spawner and unopened loot chest must be accepted");
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.OAK_PLANKS);
        check(!ScenarioGuard.check(level, box).ok(), "planks must block construction");
        helper.succeed();
    }

    /** Unknown schemas are refused, never reinterpreted or overwritten; empty data means no campaign. */
    @GameTest(template = "empty_5")
    public static void persistenceRefusesUnknownSchema(GameTestHelper helper) {
        SignalCampaignData empty = SignalCampaignData.load(new CompoundTag());
        check(!empty.isInitialized() && empty.loadError() == null, "empty data must mean 'no campaign'");

        CompoundTag future = new CompoundTag();
        future.putInt("Schema", SignalCampaignData.SCHEMA_VERSION + 1);
        future.putBoolean("Initialized", true);
        SignalCampaignData d = SignalCampaignData.load(future);
        check(d.loadError() != null && !d.isActive(), "future schema must be refused");
        d.setDirty();
        check(!d.isDirty(), "unreadable data must never be written back");

        CompoundTag bad = new CompoundTag();
        bad.putInt("Schema", SignalCampaignData.SCHEMA_VERSION);
        bad.putBoolean("Initialized", true);
        check(SignalCampaignData.load(bad).loadError() != null, "corrupt data must be refused with a message");
        helper.succeed();
    }

    /** Projection never loads chunks and never overwrites a block whose identity does not match its binding. */
    @GameTest(template = "empty_5")
    public static void projectionRespectsUnloadedAndForeignBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lamp = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos foreign = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos far = new BlockPos(29_000_000 - 64, 70, 29_000_000 - 64);
        check(!level.isLoaded(far), "test precondition: far chunk unloaded");

        level.setBlockAndUpdate(lamp, ModBlocks.SERVICE_LAMP.get().defaultBlockState());
        BlockEntity be = level.getBlockEntity(lamp);
        check(be instanceof SiteNodeBlockEntity, "lamp has no node entity");
        ((SiteNodeBlockEntity) be).setNodeId(BellwetherSite.NODE_GENERATOR);
        level.setBlockAndUpdate(foreign, Blocks.STONE.defaultBlockState());

        SignalCampaignData d = new SignalCampaignData();
        d.initialize(BellwetherScenario.ID, Level.OVERWORLD, lamp, BoundingBox.fromCorners(lamp, foreign), lamp, 0F, lamp, lamp, null,
                Map.of(lamp, BellwetherSite.NODE_GENERATOR, foreign, BellwetherSite.NODE_PUMP, far, BellwetherSite.NODE_RELAY));
        d.site().installFuse(true, d.site().bus().revision());
        d.site().closeIsolator();
        SiteProjector.project(level, d);

        check(level.getBlockState(lamp).getValue(ServiceLampBlock.LIT), "bound lamp not lit");
        check(level.getBlockState(foreign).is(Blocks.STONE), "foreign block overwritten");
        check(!level.isLoaded(far), "projection loaded a chunk");
        helper.succeed();
    }
}
