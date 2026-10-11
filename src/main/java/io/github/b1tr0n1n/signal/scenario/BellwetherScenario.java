package io.github.b1tr0n1n.signal.scenario;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import io.github.b1tr0n1n.signal.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The Bellwether relay site, built into the existing terrain. Site coordinates are relative to the
 * anchor (where the operator stands): X east, Z south. Footprint x 0..175, z -72..71.
 *
 * <ul>
 *   <li>A winding maintenance road (x 0..98) graded to the land, with puddles, mud shoulders, wrecks and a
 *       sagging power line, crossed by an abandoned rail line on its own embankment (x 66).</li>
 *   <li>The fenced service compound (x 98..149, z -24..23) on a levelled pad blended into the slope:
 *       service building, pump stand, checkpoint booth, container, collapsed shed, truck, fire barrel.</li>
 *   <li>Relay tower R4 on the highest ground north-east of the compound, reached by a footpath, climbed by
 *       a caged ladder.</li>
 *   <li>Bellwether: three lived-in houses down-slope to the south, with lit windows and chimney smoke.</li>
 * </ul>
 */
public final class BellwetherScenario {
    public static final String ID = "bellwether";
    public static final int MIN_X = 0, MAX_X = 175, MIN_Z = -72, MAX_Z = 71;
    private static final long SEED = 0x5167_4E41_4CL;

    /** Facility frame relative to the compound corner: places the service building near the compound's north side. */
    private static final int FRAME_X = 84, FRAME_Z = -24;
    private static final int CX0 = 98, CX1 = 149, CZ0 = -24, CZ1 = 23;
    /** Bellwether's house plots (see {@link #settlement}). */
    private static final int VX0 = 108, VX1 = 150, VZ0 = 38, VZ1 = 63;
    /** Most open water the compound pad and the village plots may cover before planning refuses the spot. */
    private static final double MAX_WET_COMPOUND = 0.2, MAX_WET_VILLAGE = 0.3;

    private BellwetherScenario() {
    }

    /** Pre-construction survey: everything decided from the untouched terrain. */
    public record Plan(BlockPos anchor, Terrain terrain, BoundingBox bounds, int padY, int towerX, int towerZ, int towerY,
                       @Nullable String problemKey) {
        public boolean ok() {
            return problemKey == null;
        }
    }

    public record Result(Map<BlockPos, String> bindings, BlockPos start, float startYaw, BlockPos entrance, BlockPos frame) {
    }

    /** Load the site's chunks, survey the ground and work out bounds. Writes nothing. */
    public static Plan plan(ServerLevel level, BlockPos anchor) {
        int ax = anchor.getX(), az = anchor.getZ();
        for (int cx = (ax + MIN_X) >> 4; cx <= (ax + MAX_X) >> 4; cx++) {
            for (int cz = (az + MIN_Z) >> 4; cz <= (az + MAX_Z) >> 4; cz++) {
                level.getChunk(cx, cz); // explicit operator command: generate/load the site once
            }
        }
        Terrain t = Terrain.survey(level, ax + MIN_X, az + MIN_Z, MAX_X - MIN_X + 1, MAX_Z - MIN_Z + 1);
        int padY = t.median(ax + CX0, az + CZ0, ax + CX1, az + CZ1);
        // Tower on the highest ground in the band north of the compound (clear of its pad and apron), so it
        // stands over the site as the landmark. Ties go to the spot nearest the compound's north gate.
        int bestX = ax + 150, bestZ = az - 45, best = Integer.MIN_VALUE;
        for (int x = ax + 92; x <= ax + 168; x += 2) {
            for (int z = az - 64; z <= az - 38; z += 2) {
                int h = t.maxIn(x - 3, z - 3, x + 3, z + 3) * 4 - (int) (Math.hypot(x - (ax + 130), z - (az - 30)) / 8);
                if (h > best) {
                    best = h;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        int towerY = t.maxIn(bestX - 3, bestZ - 3, bestX + 3, bestZ + 3) + 1;
        int minY = Math.min(t.min(), padY) - 4;
        int maxY = Math.max(Math.max(t.max() + 16, padY + 32), towerY + Prefabs.TOWER_TOP + 2);
        BoundingBox bounds = new BoundingBox(ax + MIN_X, minY, az + MIN_Z, ax + MAX_X, maxY, az + MAX_Z);
        String problem = null;
        if (minY < level.getMinBuildHeight() || maxY >= level.getMaxBuildHeight()) {
            problem = "command.signal.scenario.out_of_height";
        } else if (t.wetFraction(ax + CX0, az + CZ0, ax + CX1, az + CZ1) > MAX_WET_COMPOUND
                || t.wetFraction(ax + VX0, az + VZ0, ax + VX1, az + VZ1) > MAX_WET_VILLAGE) {
            // Routes cross water as causeways, but a compound or a village standing in a lake is not a site.
            problem = "command.signal.scenario.too_wet";
        }
        return new Plan(anchor.immutable(), t, bounds, padY, bestX, bestZ, towerY, problem);
    }

    public static Result build(ServerLevel level, Plan plan) {
        SiteCanvas c = new SiteCanvas(level, plan.bounds(), SEED);
        Terrain t = plan.terrain();
        int ax = plan.anchor().getX(), az = plan.anchor().getZ();
        int padY = plan.padY();
        int walk = padY + 1;

        // 1. Compound pad, blended into the slope.
        t.pad(c, ax + CX0, az + CZ0, ax + CX1, az + CZ1, padY, 8, 30, (x, z) -> yardGround(c, ax, az, x, z));
        // 2. Tower pad on its ridge.
        t.pad(c, plan.towerX() - 4, plan.towerZ() - 4, plan.towerX() + 4, plan.towerZ() + 4, plan.towerY() - 1, 4, 44,
                (x, z) -> (c.noise(x, z, 3.0) > 0.1 ? Blocks.GRAVEL : Blocks.COARSE_DIRT).defaultBlockState());

        // 3. Maintenance road: wanders through the trees and arrives level with the compound gate.
        List<Paths.Sample> road = Paths.route(t, new double[][]{
                {ax + 1.5, az + 0.5}, {ax + 16, az - 4}, {ax + 34, az + 5}, {ax + 52, az - 3}, {ax + 70, az + 4},
                {ax + 86, az - 1}, {ax + 97.5, az + 0.5}}, 0.4, 0.45, 9, Double.NaN, walk);
        Map<Long, Double> roadCells = Paths.carve(c, t, road, 1.6, 2.8, 5, Paths.ROAD, 14);
        Paths.puddles(c, roadCells, 0.05F);

        // 4. Abandoned rail line, following the land on a gentle grade, crossing the road level at x 66.
        int railX = ax + 66;
        Paths.Sample crossing = nearestX(road, railX);
        int railY = (int) crossing.height();
        int crossZ = (int) Math.floor(crossing.z());
        Map<Integer, Integer> track = new java.util.HashMap<>();
        for (int dir : new int[]{-1, 1}) {
            List<Paths.Sample> line = Paths.route(t, new double[][]{{railX + 0.5, crossZ + 0.5}, {railX + 0.5, crossZ + dir * 56 + 0.5}},
                    0.5, 0.12, 14, railY, Double.NaN);
            Map<Long, Double> cells = Paths.carve(c, t, line, 1.6, 2.6, 5, Paths.RAILBED, 7);
            cells.forEach((k, h) -> {
                if ((int) (k >> 32) == railX) {
                    track.merge(k.intValue(), (int) (double) h, Math::max);
                }
            });
        }
        layTrack(c, railX, track);
        int carZ = flatRun(track, crossZ - 12, -1, 9);
        if (carZ != Integer.MIN_VALUE) {
            Prefabs.boxcar(c, new BlockPos(railX, track.get(carZ), carZ));
        }
        int signX = railX + 4, signZ = crossZ - 4;
        c.frame(new BlockPos(signX, groundNow(level, signX, signZ) + 1, signZ));
        c.standingSign(0, 0, 0, 12, "LEVEL CROSSING", "NO SERVICE", "SINCE '09", "");

        // 5. Power line along the road into the compound.
        powerLine(c, level, road, 16, 5);

        // 6. Footpath from the compound's north gate up to the tower.
        List<Paths.Sample> path = Paths.route(t, new double[][]{
                {ax + 129.5, az - 25.5}, {ax + 131, az - 32}, {plan.towerX() - 2.0, plan.towerZ() + 9.0},
                {plan.towerX() + 0.5, plan.towerZ() + 5.5}}, 0.4, 0.5, 4, walk, plan.towerY());
        Paths.carve(c, t, path, 0.9, 1.6, 3, Paths.FOOTPATH, 10);
        powerLine(c, level, path, 14, 3);

        // 7. Tower.
        Prefabs.tower(c, new BlockPos(plan.towerX(), plan.towerY(), plan.towerZ()));

        // 8. Compound: fence, facility, yard.
        fence(c, ax + CX0, az + CZ0, ax + CX1, az + CZ1, walk);
        BlockPos frame = new BlockPos(ax + FRAME_X, walk, az + FRAME_Z);
        ServiceFacility.build(c, frame);
        yard(c, ax, az, walk, padY);

        // 9. Bellwether, down the slope to the south, with a footpath forking off the road.
        settlement(c, t, ax, az, road);

        t.dressCuts(c);
        c.containFluids();
        c.finishShapes();

        // Start a few blocks into the established road, facing along it toward the distant tower.
        Paths.Sample s0 = road.get(Math.min(road.size() - 1, (int) (8 / 0.4)));
        BlockPos start = new BlockPos((int) Math.floor(s0.x()), Paths.walkY(s0.height()), (int) Math.floor(s0.z()));
        float yaw = (float) Math.toDegrees(-Math.atan2(s0.dirX(), s0.dirZ()));
        BlockPos entrance = frame.offset(ServiceFacility.DOOR).offset(0, 0, 1);
        return new Result(Map.copyOf(c.bindings), start, yaw, entrance, frame);
    }

    // ------------------------------------------------------------------ pieces

    private static Paths.Sample nearestX(List<Paths.Sample> route, int x) {
        Paths.Sample best = route.get(0);
        for (Paths.Sample s : route) {
            if (Math.abs(s.x() - x) < Math.abs(best.x() - x)) {
                best = s;
            }
        }
        return best;
    }

    /** Rusting north-south track over the graded bed: ascending rails on every step, a few sections missing. */
    private static void layTrack(SiteCanvas c, int x, Map<Integer, Integer> track) {
        for (Map.Entry<Integer, Integer> e : track.entrySet()) {
            int z = e.getKey(), y = e.getValue();
            Integer north = track.get(z - 1), south = track.get(z + 1);
            RailShape shape = north != null && north == y + 1 ? RailShape.ASCENDING_NORTH
                    : south != null && south == y + 1 ? RailShape.ASCENDING_SOUTH : RailShape.NORTH_SOUTH;
            // The step slab laid by the bed would sit where the rail goes; the rail replaces it.
            if (c.rnd.nextFloat() > 0.07F || shape != RailShape.NORTH_SOUTH) {
                c.putSoft(x, y, z, Prefabs.rail(shape));
            } else {
                c.clear(x, y, z);
            }
        }
    }

    /** First z (scanning from {@code from} in direction {@code dir}) starting a level run of {@code len} track blocks. */
    private static int flatRun(Map<Integer, Integer> track, int from, int dir, int len) {
        for (int i = 0; i < 40; i++) {
            int z0 = from + dir * i;
            Integer y0 = track.get(z0);
            if (y0 == null) {
                continue;
            }
            boolean ok = true;
            for (int k = 1; k < len && ok; k++) {
                Integer y = track.get(z0 + dir * k);
                ok = y != null && y.equals(y0);
            }
            if (ok) {
                return z0;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Wooden poles every {@code spacing} route blocks, {@code offset} to the left of travel, wired with chain. */
    private static void powerLine(SiteCanvas c, ServerLevel level, List<Paths.Sample> route, double spacing, double offset) {
        double run = 0;
        Paths.Sample prev = route.get(0);
        BlockPos lastTop = null;
        for (Paths.Sample s : route) {
            run += Math.hypot(s.x() - prev.x(), s.z() - prev.z());
            prev = s;
            if (run < spacing) {
                continue;
            }
            run = 0;
            int x = (int) Math.floor(s.x() + s.dirZ() * offset);
            int z = (int) Math.floor(s.z() - s.dirX() * offset);
            int g = groundNow(level, x, z);
            BlockPos base = new BlockPos(x, g + 1, z);
            if (!c.bounds.isInside(base.above(8)) || !c.bounds.isInside(base)) {
                continue;
            }
            Direction.Axis axis = Math.abs(s.dirX()) >= Math.abs(s.dirZ()) ? Direction.Axis.X : Direction.Axis.Z;
            Prefabs.pole(c, base, axis);
            BlockPos top = base.above(6);
            if (lastTop != null) {
                Prefabs.wire(c, lastTop, top, 1.6);
            }
            lastTop = top;
        }
    }

    /** Current ground y, looking through plants, logs and leaves. */
    static int groundNow(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        while (y > level.getMinBuildHeight() && Terrain.isCover(level.getBlockState(p.set(x, y, z)))) {
            y--;
        }
        return y;
    }

    /** Chain-link perimeter on concrete posts, with gaps; west gate on the road, north gate to the tower path. */
    private static void fence(SiteCanvas c, int x0, int z0, int x1, int z1, int walk) {
        int gateZ = z0 + 24;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x != x0 && x != x1 && z != z0 && z != z1) {
                    continue;
                }
                if (x == x0 && Math.abs(z - gateZ) <= 3 || z == z0 && x >= x0 + 29 && x <= x0 + 33) {
                    continue;
                }
                boolean post = x == x0 || x == x1 ? (z - z0) % 4 == 0 : (x - x0) % 4 == 0;
                if (post) {
                    c.put(x, walk, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                    c.put(x, walk + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                    c.put(x, walk + 2, z, Blocks.IRON_BARS.defaultBlockState());
                } else if (c.rnd.nextFloat() > 0.12F) {
                    c.put(x, walk, z, Blocks.IRON_BARS.defaultBlockState());
                    if (c.rnd.nextFloat() > 0.2F) {
                        c.put(x, walk + 1, z, Blocks.IRON_BARS.defaultBlockState());
                    }
                }
            }
        }
        for (int dz : new int[]{-4, 4}) {
            for (int y = 0; y <= 2; y++) {
                c.put(x0, walk + y, gateZ + dz, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            }
            c.put(x0, walk + 3, gateZ + dz, Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void yard(SiteCanvas c, int ax, int az, int walk, int padY) {
        // Worn vehicle track from the gate to the door: two muddy wheel ruts either side of a packed crown.
        double[] a = {ax + 98.5, az + 0.5}, b = {ax + 111.5, az - 8.5};
        double len = Math.hypot(b[0] - a[0], b[1] - a[1]), ux = (b[0] - a[0]) / len, uz = (b[1] - a[1]) / len;
        for (int xx = (int) Math.min(a[0], b[0]) - 3; xx <= (int) Math.max(a[0], b[0]) + 3; xx++) {
            for (int zz = (int) Math.min(a[1], b[1]) - 3; zz <= (int) Math.max(a[1], b[1]) + 3; zz++) {
                double rx = xx + 0.5 - a[0], rz = zz + 0.5 - a[1];
                double along = rx * ux + rz * uz, across = Math.abs(rx * uz - rz * ux);
                if (along < 0 || along > len || across > 2.0 || !c.get(xx, walk, zz).isAir()) {
                    continue;
                }
                c.put(xx, padY, zz, (across > 0.9 ? Blocks.MUD : Blocks.PACKED_MUD).defaultBlockState());
            }
        }
        int cx = ax + CX0, cz = az + CZ0;
        Prefabs.truck(c, new BlockPos(cx + 8, walk, cz + 34));
        Prefabs.container(c, new BlockPos(cx + 34, walk, cz + 32));
        Prefabs.collapsedShed(c, new BlockPos(cx + 42, walk, cz + 4));
        Prefabs.booth(c, new BlockPos(cx + 2, walk, cz + 28), Direction.WEST);
        Prefabs.fireBarrel(c, new BlockPos(cx + 24, walk, cz + 30));
        for (int[] l : new int[][]{{14, 22}, {30, 21}, {4, 18}, {46, 30}}) {
            Prefabs.lampPost(c, new BlockPos(cx + l[0], walk, cz + l[1]));
        }
        // Drums and pallets by the pump stand.
        c.frame(new BlockPos(cx + 31, walk, cz + 12));
        c.set(0, 0, 0, Blocks.BARREL);
        c.set(1, 0, 0, Blocks.CAULDRON);
        c.set(0, 0, 1, Blocks.BARREL);
        c.set(0, 1, 0, Blocks.BARREL);
        stash(c, c.rel(0, 0, 0), new ItemStack(ModItems.REVOLVER_ROUND.get(), 2)); // someone's last two rounds
        c.frame(new BlockPos(cx + 44, walk, cz + 34));
        for (int y = 0; y <= 2; y++) {
            c.set(0, y, 0, Prefabs.slab(Blocks.SPRUCE_SLAB, y == 2 ? SlabType.BOTTOM : SlabType.DOUBLE));
            c.set(1, y, 0, Prefabs.slab(Blocks.SPRUCE_SLAB, y == 2 ? SlabType.BOTTOM : SlabType.DOUBLE));
        }
        // Rain puddles in the mud hollows: standing water held on every side by the pad, nothing above it.
        for (int x = cx + 3; x <= ax + CX1 - 3; x++) {
            for (int z = cz + 3; z <= az + CZ1 - 3; z++) {
                if (c.noise(x, z, 15.0) > -0.62 || c.noise(x + 500, z - 300, 5.0) > -0.1
                        || !c.get(x, padY, z).is(Blocks.MUD) || !c.get(x, walk, z).isAir()) {
                    continue;
                }
                boolean held = c.get(x, padY - 1, z).isSolid();
                for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    BlockState side = c.get(x + o[0], padY, z + o[1]);
                    held &= side.isSolid() || side.is(Blocks.WATER);
                }
                if (held) {
                    c.put(x, padY, z, Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    /**
     * Compound ground: weathered tuff hardstanding broken up by bare packed earth, dark mud hollows where the
     * rain stands, and grass and podzol creeping in along the fence. Broad, low-contrast patches.
     */
    private static BlockState yardGround(SiteCanvas c, int ax, int az, int x, int z) {
        int edge = Math.min(Math.min(x - (ax + CX0), (ax + CX1) - x), Math.min(z - (az + CZ0), (az + CZ1) - z));
        double big = c.noise(x, z, 15.0), mid = c.noise(x + 500, z - 300, 5.0);
        if (edge <= 1 || edge == 2 && mid > 0.2) {
            return (mid < -0.3 ? Blocks.PODZOL : Blocks.GRASS_BLOCK).defaultBlockState();
        }
        if (big < -0.5 || big < -0.3 && mid < 0.0) {
            return Blocks.MUD.defaultBlockState();
        }
        if (big > 0.5 && mid > -0.1) {
            return Blocks.PACKED_MUD.defaultBlockState();
        }
        return (mid > 0.75 ? Blocks.ANDESITE : Blocks.TUFF).defaultBlockState();
    }

    /** A house plot in Bellwether: north-west wall corner and the house drawn on it. */
    private record Plot(int x, int z, Prefabs.House house) {
    }

    /**
     * Bellwether: three houses on levelled plots down the slope to the south, joined by a lane that forks off
     * the road and runs past each front door. Plots are levelled first, then the lanes are carved (their
     * aprons never re-grade a plot), and only then are the houses drawn, so no cut can reach a wall.
     */
    private static void settlement(SiteCanvas c, Terrain t, int ax, int az, List<Paths.Sample> road) {
        Plot[] plots = {
                new Plot(ax + 110, az + 44, new Prefabs.House(7, true, true, true)),
                new Plot(ax + 124, az + 52, new Prefabs.House(9, true, false, true)),
                new Plot(ax + 140, az + 41, new Prefabs.House(7, false, false, false))};
        int[] floor = new int[plots.length];
        for (int i = 0; i < plots.length; i++) {
            Plot p = plots[i];
            int w = p.house().width();
            floor[i] = t.median(p.x(), p.z(), p.x() + w - 1, p.z() + 5);
            t.pad(c, p.x() - 2, p.z() - 3, p.x() + w + 1, p.z() + p.house().backRows() - 1, floor[i], 4, 12,
                    (x, z) -> {
                        double n = c.noise(x, z, 6.0);
                        return (n > 0.45 ? Blocks.COARSE_DIRT : n < -0.5 ? Blocks.PODZOL : Blocks.GRASS_BLOCK).defaultBlockState();
                    });
        }
        // The lane: road fork -> each doorstep in turn, each leg pinned to the levels it joins.
        Paths.Sample fork = nearestX(road, ax + 86);
        double[] from = {fork.x(), fork.z() + 2.0};
        double fromH = fork.height();
        for (int i = 0; i < plots.length; i++) {
            Plot p = plots[i];
            double[] step = {p.x() + p.house().width() / 2 + 0.5, p.z() - 1.5};
            double[][] via = i == 0 ? new double[][]{from, {ax + 94, az + 26}, {ax + 104, az + 38}, step} : new double[][]{from, step};
            List<Paths.Sample> lane = Paths.route(t, via, 0.4, 0.5, 5, fromH, floor[i] + 1);
            Paths.carve(c, t, lane, 0.9, 1.7, 3, Paths.FOOTPATH, 10);
            from = step;
            fromH = floor[i] + 1;
        }
        for (int i = 0; i < plots.length; i++) {
            Plot p = plots[i];
            Prefabs.house(c, new BlockPos(p.x(), floor[i] + 1, p.z()), p.house(), true);
            if (p.house().lived()) {
                // What a household can spare on these islands: a little food, a round or two.
                stash(c, new BlockPos(p.x() + p.house().width() - 2, floor[i] + 1, p.z() + 1),
                        new ItemStack(ModItems.REVOLVER_ROUND.get(), i == 0 ? 2 : 1), new ItemStack(Items.BREAD, 2));
            }
            if (p.house().lived()) {
                Prefabs.lampPost(c, new BlockPos(p.x() - 2, floor[i] + 1, p.z() - 1));
            }
        }
        int fx = (int) Math.floor(fork.x()) + 2, fz = (int) Math.floor(fork.z()) + 4;
        int metres = (int) Math.round(Math.hypot(plots[0].x() + 3 - fx, plots[0].z() + 3 - fz) / 10.0) * 10;
        c.frame(new BlockPos(fx, groundNow(c.level, fx, fz) + 1, fz));
        c.standingSign(0, 0, 0, 8, "BELLWETHER", "<- " + metres + " M", "WATER - REPAIRS", "");
        Prefabs.carWreck(c, new BlockPos(ax + 44, groundNow(c.level, ax + 44, az + 8) + 1, az + 8));
    }

    /**
     * Put loot in a container the site just built. Ammunition is scarce everywhere: the whole site holds five
     * rounds (decision #46). Does nothing if the block is not a container.
     */
    static void stash(SiteCanvas c, BlockPos pos, ItemStack... items) {
        if (c.level.getBlockEntity(pos) instanceof net.minecraft.world.Container box) {
            int slot = 0;
            for (ItemStack s : items) {
                box.setItem(4 + slot++ * 7 % box.getContainerSize(), s);
            }
        }
    }

    static BlockState slab(net.minecraft.world.level.block.Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }
}
