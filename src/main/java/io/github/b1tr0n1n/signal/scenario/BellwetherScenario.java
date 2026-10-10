package io.github.b1tr0n1n.signal.scenario;

import io.github.b1tr0n1n.signal.block.SiteMachineBlock;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import io.github.b1tr0n1n.signal.campaign.SiteProjector;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.registry.ModBlocks;
import io.github.b1tr0n1n.signal.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Procedural, authored construction of the Bellwether relay site (milestone one): a forest maintenance
 * road with a drainage channel, service sign and abandoned maintenance truck; a gravel service yard; one
 * compact service room with generator, distribution panel, pump and relay controllers, local transmitter,
 * spare-fuse cabinet and maintenance log; an exterior pump header tank; and a lattice relay tower.
 *
 * <p>All coordinates are relative to the anchor (X east, Z south, Y up; dy 0 is the walking level).
 * Every write stays inside {@link #bounds(BlockPos)}. Placement uses known-shape updates so nothing
 * cascades outside the site; a final pass connects bars, panes, fences and walls.
 */
public final class BellwetherScenario {
    public static final String ID = "bellwether";
    public static final int SIZE_X = 48;
    public static final int SIZE_Z = 28;
    public static final int DEPTH = 3;
    public static final int HEIGHT = 30;
    /** Fixed seed: the same site every time. */
    private static final long SEED = 0x5167_4E41_4CL;

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private final ServerLevel level;
    private final BlockPos anchor;
    private final RandomSource rnd = RandomSource.create(SEED);
    private final List<BlockPos> needsShape = new ArrayList<>();
    private final Map<BlockPos, String> bindings = new LinkedHashMap<>();

    public record Result(Map<BlockPos, String> bindings, BlockPos start, float startYaw, BlockPos entrance) {
    }

    private BellwetherScenario(ServerLevel level, BlockPos anchor) {
        this.level = level;
        this.anchor = anchor;
    }

    public static BoundingBox bounds(BlockPos anchor) {
        return new BoundingBox(anchor.getX(), anchor.getY() - DEPTH, anchor.getZ(),
                anchor.getX() + SIZE_X - 1, anchor.getY() + HEIGHT, anchor.getZ() + SIZE_Z - 1);
    }

    public static Result build(ServerLevel level, BlockPos anchor) {
        BellwetherScenario b = new BellwetherScenario(level, anchor);
        b.groundAndClear();
        b.road();
        b.vehicle();
        b.yard();
        b.serviceRoom();
        b.pumpStation();
        b.tower();
        b.trees();
        b.vegetation();
        b.finishShapes();
        return new Result(Map.copyOf(b.bindings), b.rel(2, 0, 16), -90.0F, b.rel(27, 0, 15));
    }

    // ------------------------------------------------------------------ primitives

    private BlockPos rel(int dx, int dy, int dz) {
        return anchor.offset(dx, dy, dz);
    }

    private boolean inBounds(int dx, int dy, int dz) {
        return dx >= 0 && dx < SIZE_X && dz >= 0 && dz < SIZE_Z && dy >= -DEPTH && dy <= HEIGHT;
    }

    private void set(int dx, int dy, int dz, BlockState s) {
        if (!inBounds(dx, dy, dz)) {
            throw new IllegalStateException("Scenario write outside bounds: " + dx + "," + dy + "," + dz);
        }
        BlockPos p = rel(dx, dy, dz);
        level.setBlock(p, s, FLAGS);
        if (s.getBlock() instanceof CrossCollisionBlock || s.getBlock() instanceof WallBlock) {
            needsShape.add(p);
        }
    }

    private void set(int dx, int dy, int dz, Block b) {
        set(dx, dy, dz, b.defaultBlockState());
    }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(x, y, z, s);
                }
            }
        }
    }

    private BlockState pick(Block a, float pa, Block b, float pb, Block c) {
        float r = rnd.nextFloat();
        if (r < pa) {
            return a.defaultBlockState();
        }
        if (r < pa + pb) {
            return b.defaultBlockState();
        }
        return c.defaultBlockState();
    }

    private void bindMachine(int dx, int dy, int dz, Block block, Direction facing, String node) {
        set(dx, dy, dz, block.defaultBlockState().setValue(SiteMachineBlock.FACING, facing));
        bind(dx, dy, dz, node);
    }

    private void bindLamp(int dx, int dy, int dz, String node) {
        set(dx, dy, dz, ModBlocks.SERVICE_LAMP.get().defaultBlockState());
        bind(dx, dy, dz, node);
    }

    private void bind(int dx, int dy, int dz, String node) {
        BlockPos p = rel(dx, dy, dz);
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof SiteNodeBlockEntity site) {
            site.setNodeId(node);
        }
        bindings.put(p, node);
    }

    private SignText text(String... lines) {
        SignText t = new SignText();
        for (int i = 0; i < lines.length && i < 4; i++) {
            t = t.setMessage(i, Component.literal(lines[i]));
        }
        return t;
    }

    private void writeSign(BlockPos p, String... lines) {
        if (level.getBlockEntity(p) instanceof SignBlockEntity sign) {
            sign.setText(text(lines), true);
            sign.setWaxed(true);
        }
    }

    private void standingSign(int dx, int dy, int dz, int rotation, String... lines) {
        set(dx, dy, dz, Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        writeSign(rel(dx, dy, dz), lines);
    }

    /** Wall sign occupying (dx,dy,dz), attached to the block behind it, readable from {@code facing}. */
    private void wallSign(int dx, int dy, int dz, Direction facing, String... lines) {
        set(dx, dy, dz, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        writeSign(rel(dx, dy, dz), lines);
    }

    private BlockState slab(Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    // ------------------------------------------------------------------ terrain

    private void groundAndClear() {
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                set(x, -3, z, Blocks.DIRT);
                set(x, -2, z, rnd.nextFloat() < 0.15F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                float r = rnd.nextFloat();
                BlockState surface = r < 0.14F ? Blocks.PODZOL.defaultBlockState()
                        : r < 0.24F ? Blocks.COARSE_DIRT.defaultBlockState()
                        : r < 0.29F ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                set(x, -1, z, surface);
                for (int y = 0; y <= HEIGHT; y++) {
                    set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private void road() {
        // Maintenance road, west edge to the yard. Worn centre line, muddy shoulders.
        for (int x = 0; x <= 21; x++) {
            set(x, -1, 16, Blocks.DIRT_PATH);
            set(x, -1, 15, pick(Blocks.COARSE_DIRT, 0.5F, Blocks.MUD, 0.25F, Blocks.GRAVEL));
            set(x, -1, 17, pick(Blocks.COARSE_DIRT, 0.5F, Blocks.MUD, 0.25F, Blocks.GRAVEL));
        }
        // Drainage channel parallel to the road, closed at both ends so the water stays put.
        for (int x = 1; x <= 19; x++) {
            set(x, -2, 19, Blocks.MUD);
            set(x, -1, 19, Blocks.WATER);
            set(x, -1, 18, pick(Blocks.MUD, 0.4F, Blocks.MOSSY_COBBLESTONE, 0.2F, Blocks.COARSE_DIRT));
            set(x, -1, 20, pick(Blocks.MUD, 0.3F, Blocks.MOSSY_COBBLESTONE, 0.3F, Blocks.GRASS_BLOCK));
        }
        set(0, -1, 19, Blocks.MOSSY_COBBLESTONE);
        set(20, -1, 19, Blocks.MOSSY_COBBLESTONE);
        // Culvert grate where the channel meets the yard.
        set(20, 0, 19, Blocks.IRON_BARS);

        standingSign(6, 0, 14, 0, "BELLWETHER RELAY", "STATION 4  →", "SERVICE YARD", "C.R.N. MAINT. RD 7");
    }

    private void vehicle() {
        // Abandoned maintenance truck, nose east, one tyre flat.
        set(10, 0, 11, Blocks.POLISHED_BLACKSTONE);
        set(10, 0, 13, Blocks.POLISHED_BLACKSTONE);
        set(14, 0, 11, Blocks.POLISHED_BLACKSTONE);
        set(14, 0, 13, slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM));
        fill(10, 1, 11, 14, 1, 13, Blocks.GREEN_TERRACOTTA.defaultBlockState());
        set(11, 1, 12, Blocks.BROWN_TERRACOTTA); // rust through the bed
        for (int x = 10; x <= 12; x++) {
            set(x, 2, 11, Blocks.SPRUCE_FENCE);
            set(x, 2, 13, Blocks.SPRUCE_FENCE);
        }
        set(13, 2, 11, Blocks.GREEN_TERRACOTTA);
        set(13, 2, 13, Blocks.GREEN_TERRACOTTA);
        for (int z = 11; z <= 13; z++) {
            set(14, 2, z, Blocks.GLASS_PANE);
            set(13, 3, z, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
            set(14, 3, z, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        }
        set(11, 2, 12, Blocks.BARREL);
    }

    private void yard() {
        for (int x = 20; x <= 37; x++) {
            for (int z = 2; z <= 24; z++) {
                set(x, -1, z, pick(Blocks.GRAVEL, 0.55F, Blocks.ANDESITE, 0.25F, Blocks.COARSE_DIRT));
            }
        }
        // Lantern posts: yard entrance and beside the door.
        lanternPost(20, 14);
        lanternPost(30, 16);
    }

    private void lanternPost(int x, int z) {
        set(x, 0, z, Blocks.SPRUCE_FENCE);
        set(x, 1, z, Blocks.SPRUCE_FENCE);
        set(x, 2, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    // ------------------------------------------------------------------ service room

    private void serviceRoom() {
        int x0 = 22, x1 = 34, z0 = 4, z1 = 14;
        // Footing and floor.
        fill(x0, -1, z0, x1, -1, z1, Blocks.STONE_BRICKS.defaultBlockState());
        fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
        // Walls: weathered concrete-like stone brick, damp at the base.
        for (int y = 0; y <= 4; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    if (!edge) {
                        continue;
                    }
                    float mossChance = y == 0 ? 0.3F : y == 1 ? 0.1F : 0.02F;
                    set(x, y, z, pick(Blocks.MOSSY_STONE_BRICKS, mossChance, Blocks.CRACKED_STONE_BRICKS, 0.1F, Blocks.STONE_BRICKS));
                }
            }
        }
        // Roof with a weathered copper edge.
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean rim = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
                set(x, 5, z, slab(rim ? Blocks.WEATHERED_CUT_COPPER_SLAB : Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
            }
        }
        // Doorway (south), open: the yard leads straight in.
        fill(27, 0, z1, 28, 2, z1, Blocks.AIR.defaultBlockState());
        wallSign(27, 3, z1 + 1, Direction.SOUTH, "BELLWETHER", "RELAY STN. 4", "SERVICE ROOM", "");
        // Windows; one pane boarded over, one cracked open.
        for (int z = 7; z <= 11; z++) {
            set(x0, 2, z, z == 9 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
        }
        for (int z = 10; z <= 12; z++) {
            set(x1, 2, z, Blocks.GLASS_PANE);
        }
        set(24, 2, z1, Blocks.GLASS_PANE);
        set(25, 2, z1, Blocks.GLASS_PANE);
        set(31, 2, z1, Blocks.GLASS_PANE);
        set(32, 2, z1, Blocks.AIR);

        // Generator exhaust through the roof.
        for (int y = 0; y <= 4; y++) {
            set(23, y, 5, Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        }
        set(23, 5, 5, Blocks.IRON_BARS);
        set(23, 6, 5, Blocks.IRON_BARS);

        // North equipment wall, everything facing south toward the room.
        bindMachine(24, 0, 5, ModBlocks.GENERATOR.get(), Direction.SOUTH, BellwetherSite.NODE_GENERATOR);
        bindLamp(24, 1, 5, BellwetherSite.NODE_GENERATOR);
        wallSign(25, 1, 5, Direction.SOUTH, "GENERATOR G1", "RATED 6 UNITS", "USE: CLOSE ISOL.", "SNEAK: ISOLATE");

        set(27, 0, 5, Blocks.SMOOTH_STONE);
        bindMachine(27, 1, 5, ModBlocks.DISTRIBUTION_PANEL.get(), Direction.SOUTH, BellwetherSite.NODE_PANEL);
        wallSign(28, 1, 5, Direction.SOUTH, "PANEL DP-1", "MAIN FUSE F1", "USE TO OPEN", "");

        set(30, 0, 5, Blocks.SMOOTH_STONE);
        bindMachine(30, 1, 5, ModBlocks.PUMP_CONTROLLER.get(), Direction.SOUTH, BellwetherSite.NODE_PUMP);
        bindLamp(30, 2, 5, BellwetherSite.NODE_PUMP);
        wallSign(31, 1, 5, Direction.SOUTH, "PUMP P1", "5 UNITS", "GAUGE: PRESSURE", "");

        set(32, 0, 5, Blocks.SMOOTH_STONE);
        bindMachine(32, 1, 5, ModBlocks.RELAY_CONTROLLER.get(), Direction.SOUTH, BellwetherSite.NODE_RELAY);
        bindLamp(32, 2, 5, BellwetherSite.NODE_RELAY);
        wallSign(33, 1, 5, Direction.SOUTH, "REGIONAL RELAY", "R4 - 3 UNITS", "ANTENNA: TOWER", "");

        // Fresh cable tray along the old wall.
        for (int x = 24; x <= 33; x++) {
            set(x, 3, 5, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.X));
        }

        // Spare fuse cabinet on the west wall: two fuses, so a correct repair visibly uses exactly one.
        set(23, 0, 8, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(rel(23, 0, 8)) instanceof Container c) {
            c.setItem(13, new ItemStack(ModItems.REPLACEMENT_FUSE.get(), 2));
        }
        wallSign(23, 1, 8, Direction.EAST, "SPARE FUSES", "TYPE F1", "", "");

        // Work bench with a lit candle.
        set(23, 0, 10, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        set(23, 0, 11, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        set(23, 1, 11, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 2));

        // Local transmitter desk on the east wall.
        set(33, 0, 9, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        set(33, 0, 10, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        bindLamp(33, 1, 9, BellwetherSite.NODE_LOCAL_TX);
        set(33, 2, 9, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        wallSign(33, 1, 10, Direction.WEST, "LOCAL TX", "1 UNIT", "BELLWETHER", "MAINT. CHANNEL");

        // Nia's corner: bedroll and a cold heater.
        set(30, 0, 12, Blocks.WHITE_CARPET);
        set(31, 0, 12, Blocks.GREEN_CARPET);
        set(32, 0, 12, Blocks.GREEN_CARPET);
        set(33, 0, 13, Blocks.BLAST_FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.WEST));

        // Maintenance log at the entry.
        set(25, 0, 11, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST));
        LecternBlock.tryPlaceBook(null, level, rel(25, 0, 11), level.getBlockState(rel(25, 0, 11)), maintenanceLog());

        // Practical light.
        set(26, 4, 8, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(31, 4, 9, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private static ItemStack maintenanceLog() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : new String[]{
                "BELLWETHER RELAY STN. 4\nMaintenance log\n\nGenerator G1: 6 units\nPump P1: 5\nLocal TX: 1\nRegional relay R4: 3\n\nThe bus carries what G1 gives. No more.",
                "Day 212\nF1 running hot. Isolated the supply at G1 before it went. Pump's on the gravity bypass for now.\n\nWe have water. We can't tell the depot what we need.\n- N.S.",
                "F1 REPLACEMENT\n1. Confirm G1 isolated.\n2. Spare fuse from the cabinet by the bench.\n3. Fit F1 at DP-1.\n4. Close the isolator at G1.\n5. Set breakers at DP-1.\n   Check the total first.",
                "Day 209\nStorm knocked R4's antenna off its bearing. Relay will take power and reach nobody until it's realigned.\n\nDon't waste the units on it.\n- N.S."}) {
            pages.add(Filterable.passThrough(Component.literal(page)));
        }
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Maintenance Log"), "N. Serrat", 0, pages, true));
        return book;
    }

    // ------------------------------------------------------------------ pump service view

    private void pumpStation() {
        fill(35, -1, 9, 39, -1, 13, Blocks.POLISHED_ANDESITE.defaultBlockState());
        // Feed pipe out of the service room wall.
        set(35, 1, 11, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.EAST));
        set(36, 1, 11, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.EAST));
        set(37, 0, 11, Blocks.OXIDIZED_CUT_COPPER);
        set(37, 1, 11, slab(Blocks.OXIDIZED_CUT_COPPER_SLAB, SlabType.BOTTOM));
        // Header tank: water level mirrors pump pressure.
        set(38, 0, 10, Blocks.CAULDRON);
        set(38, 0, 11, Blocks.CAULDRON);
        bindings.put(rel(38, 0, 10), SiteProjector.NODE_PUMP_TANK);
        bindings.put(rel(38, 0, 11), SiteProjector.NODE_PUMP_TANK);
        set(36, 0, 13, Blocks.STONE_BRICK_WALL);
        bindLamp(36, 1, 13, BellwetherSite.NODE_PUMP);
        standingSign(38, 0, 13, 0, "PUMP SERVICE", "HEADER TANK", "TO BELLWETHER", "");
    }

    // ------------------------------------------------------------------ relay tower

    private void tower() {
        fill(38, -1, 1, 44, -1, 7, Blocks.SMOOTH_STONE.defaultBlockState());
        BlockState leg = Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
        // Lower section: 5x5 footprint.
        int[][] lower = {{39, 2}, {43, 2}, {39, 6}, {43, 6}};
        for (int y = 0; y <= 9; y++) {
            for (int[] c : lower) {
                set(c[0], y, c[1], leg);
            }
        }
        for (int y : new int[]{3, 6, 9}) {
            for (int x = 40; x <= 42; x++) {
                set(x, y, 2, Blocks.IRON_BARS);
                set(x, y, 6, Blocks.IRON_BARS);
            }
            for (int z = 3; z <= 5; z++) {
                set(39, y, z, Blocks.IRON_BARS);
                set(43, y, z, Blocks.IRON_BARS);
            }
        }
        BlockState grate = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false);
        fill(39, 10, 2, 43, 10, 6, grate);
        // Upper section: 3x3 footprint.
        int[][] upper = {{40, 3}, {42, 3}, {40, 5}, {42, 5}};
        for (int y = 11; y <= 17; y++) {
            for (int[] c : upper) {
                set(c[0], y, c[1], leg);
            }
        }
        for (int y : new int[]{14, 17}) {
            set(41, y, 3, Blocks.IRON_BARS);
            set(41, y, 5, Blocks.IRON_BARS);
            set(40, y, 4, Blocks.IRON_BARS);
            set(42, y, 4, Blocks.IRON_BARS);
        }
        fill(40, 18, 3, 42, 18, 5, grate);
        // Mast, antenna panel (knocked off its bearing: canted, one panel missing) and beacon.
        for (int y = 19; y <= 25; y++) {
            set(41, y, 4, leg);
        }
        set(40, 21, 5, Blocks.WEATHERED_CUT_COPPER);
        set(41, 21, 5, Blocks.WEATHERED_CUT_COPPER);
        set(41, 22, 5, Blocks.WEATHERED_CUT_COPPER);
        set(42, 20, 5, slab(Blocks.WEATHERED_CUT_COPPER_SLAB, SlabType.TOP));
        bindLamp(41, 26, 4, BellwetherSite.NODE_RELAY);
        set(41, 27, 4, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
    }

    // ------------------------------------------------------------------ forest

    private void trees() {
        int[][] trees = {
                {3, 3, 9}, {8, 7, 8}, {14, 3, 10}, {17, 9, 7}, {3, 10, 7},
                {4, 24, 8}, {11, 23, 10}, {18, 25, 9}, {26, 25, 8}, {33, 24, 9}, {41, 23, 10}, {45, 25, 7},
                {45, 10, 9}, {45, 17, 8}};
        for (int[] t : trees) {
            spruce(t[0], t[1], t[2]);
        }
    }

    private void spruce(int x, int z, int h) {
        BlockState leaves = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
        for (int y = 2; y <= h; y++) {
            int fromTop = h - y;
            int r = fromTop <= 1 ? 1 : (fromTop % 2 == 0 ? 2 : 1);
            if (y == 2) {
                r = 1;
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > r + (r == 2 ? 1 : 0)) {
                        continue;
                    }
                    if (inBounds(x + dx, y, z + dz)) {
                        set(x + dx, y, z + dz, leaves);
                    }
                }
            }
        }
        set(x, h + 1, z, leaves);
        for (int y = 0; y < h; y++) {
            set(x, y, z, Blocks.SPRUCE_LOG);
        }
    }

    private void vegetation() {
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                BlockState ground = level.getBlockState(rel(x, -1, z));
                if (!level.getBlockState(rel(x, 0, z)).isAir()) {
                    continue;
                }
                if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.PODZOL) || ground.is(Blocks.MOSS_BLOCK))) {
                    continue;
                }
                float r = rnd.nextFloat();
                if (r < 0.22F) {
                    set(x, 0, z, Blocks.SHORT_GRASS);
                } else if (r < 0.30F) {
                    set(x, 0, z, Blocks.FERN);
                } else if (r < 0.33F && ground.is(Blocks.MOSS_BLOCK)) {
                    set(x, 0, z, Blocks.MOSS_CARPET);
                } else if (r < 0.34F) {
                    set(x, 0, z, Blocks.MOSSY_COBBLESTONE);
                }
            }
        }
    }

    private void finishShapes() {
        for (BlockPos p : needsShape) {
            BlockState s = level.getBlockState(p);
            BlockState updated = Block.updateFromNeighbourShapes(s, level, p);
            if (updated != s) {
                level.setBlock(p, updated, FLAGS);
            }
        }
    }
}
