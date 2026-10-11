package io.github.b1tr0n1n.signal.scenario;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Terrain-following routes: a Catmull-Rom spline through waypoints, sampled densely, with heights taken
 * from the surveyed ground, smoothed along the route and limited in grade. The walking surface is graded
 * in half blocks (slabs) so the player never has to jump a full step.
 */
final class Paths {
    /** One sample along a route: position, unit direction, and walking-surface height (in half blocks). */
    record Sample(double x, double z, double dirX, double dirZ, double height) {
    }

    record Style(Block core, Block coreAlt, Block coreSlab, Block shoulder, Block shoulderAlt, Block shoulderSlab) {
    }

    static final Style ROAD = new Style(Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.ANDESITE_SLAB,
            Blocks.MUD, Blocks.PODZOL, Blocks.MUD_BRICK_SLAB);
    static final Style FOOTPATH = new Style(Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.ANDESITE_SLAB,
            Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MUD_BRICK_SLAB);
    static final Style RAILBED = new Style(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.ANDESITE_SLAB,
            Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.ANDESITE_SLAB);

    private Paths() {
    }

    static List<Sample> route(Terrain t, double[][] waypoints, double step, double maxGradePerBlock, int smoothRadius,
                              double pinStart, double pinEnd) {
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < waypoints.length - 1; i++) {
            double[] p0 = waypoints[Math.max(0, i - 1)], p1 = waypoints[i], p2 = waypoints[i + 1],
                    p3 = waypoints[Math.min(waypoints.length - 1, i + 2)];
            double len = Math.hypot(p2[0] - p1[0], p2[1] - p1[1]);
            int n = Math.max(1, (int) Math.ceil(len / step));
            for (int k = 0; k < n; k++) {
                double u = k / (double) n;
                pts.add(catmull(p0, p1, p2, p3, u));
            }
        }
        pts.add(waypoints[waypoints.length - 1]);

        int n = pts.size();
        double[] h = new double[n];
        for (int i = 0; i < n; i++) {
            h[i] = t.level((int) Math.floor(pts.get(i)[0]), (int) Math.floor(pts.get(i)[1])) + 1.0;
        }
        // Moving average, then two-pass grade limit.
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            int c = 0;
            for (int j = Math.max(0, i - smoothRadius); j <= Math.min(n - 1, i + smoothRadius); j++) {
                sum += h[j];
                c++;
            }
            s[i] = sum / c;
        }
        double maxStep = maxGradePerBlock * step;
        if (!Double.isNaN(pinStart)) {
            s[0] = pinStart;
        }
        if (!Double.isNaN(pinEnd)) {
            s[n - 1] = pinEnd;
        }
        for (int i = 1; i < n; i++) {
            s[i] = Math.max(s[i - 1] - maxStep, Math.min(s[i - 1] + maxStep, s[i]));
        }
        if (!Double.isNaN(pinEnd)) {
            s[n - 1] = pinEnd;
        }
        for (int i = n - 2; i >= 0; i--) {
            s[i] = Math.max(s[i + 1] - maxStep, Math.min(s[i + 1] + maxStep, s[i]));
        }
        List<Sample> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] a = pts.get(Math.max(0, i - 1)), b = pts.get(Math.min(n - 1, i + 1));
            double dx = b[0] - a[0], dz = b[1] - a[1], len = Math.max(1e-6, Math.hypot(dx, dz));
            out.add(new Sample(pts.get(i)[0], pts.get(i)[1], dx / len, dz / len, Math.round(s[i])));
        }
        return out;
    }

    private static double[] catmull(double[] p0, double[] p1, double[] p2, double[] p3, double u) {
        double u2 = u * u, u3 = u2 * u;
        double[] r = new double[2];
        for (int k = 0; k < 2; k++) {
            r[k] = 0.5 * ((2 * p1[k]) + (-p0[k] + p2[k]) * u + (2 * p0[k] - 5 * p1[k] + 4 * p2[k] - p3[k]) * u2
                    + (-p0[k] + 3 * p1[k] - 3 * p2[k] + p3[k]) * u3);
        }
        return r;
    }

    private record Cell(double dist, double height, boolean core) {
    }

    /**
     * Carve a route: a core and shoulders at the route's walking height, then an {@code apron} that blends
     * back to the surveyed ground so cuts and fills never leave walls. Heights are whole blocks; where a cell
     * is one lower than a neighbour, a slab is laid on it as a ramp step so the player never has to jump.
     * Surfaces come in coherent patches (noise), not per-block confetti. Returns walking height per cell.
     */
    static Map<Long, Double> carve(SiteCanvas c, Terrain t, List<Sample> route, double coreHalf, double shoulderHalf,
                                   int apron, Style style, int headroom) {
        Map<Long, Cell> cells = new HashMap<>();
        for (Sample s : route) {
            double px = -s.dirZ(), pz = s.dirX();
            for (double w = -(shoulderHalf + apron); w <= shoulderHalf + apron; w += 0.5) {
                int x = (int) Math.floor(s.x() + px * w);
                int z = (int) Math.floor(s.z() + pz * w);
                long key = key(x, z);
                double d = Math.abs(w);
                Cell old = cells.get(key);
                if (old == null || d < old.dist) {
                    cells.put(key, new Cell(d, s.height(), d <= coreHalf));
                }
            }
        }
        Map<Long, Double> heights = new HashMap<>();
        for (Map.Entry<Long, Cell> e : cells.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = e.getKey().intValue();
            Cell cell = e.getValue();
            int walk = (int) cell.height;
            if (cell.dist <= shoulderHalf) {
                double n = c.noise(x, z, 5.0);
                Block main = cell.core ? (n > 0.25 ? style.coreAlt() : style.core())
                        : (n > 0.0 ? style.shoulderAlt() : style.shoulder());
                t.shape(c, x, z, walk - 1, main.defaultBlockState(), walk + headroom);
                heights.put(e.getKey(), (double) walk);
            } else if (!t.isLevelled(x, z)) {
                // Apron: ease from the route surface back to the original ground (never across a levelled pad).
                int g = t.level(x, z);
                int k = (int) Math.ceil(cell.dist - shoulderHalf);
                int target = (int) Math.round((walk - 1) + (g - (walk - 1)) * Terrain.blend(k, apron));
                if (target != g && !(t.wetAt(x, z) && target <= g)) { // never dig into standing water
                    t.shape(c, x, z, target, Terrain.verge(c, x, z, k), Math.max(target, g) + headroom / 2);
                }
            }
        }
        // Ramp steps: a slab on any route cell that sits one below a neighbouring route cell.
        for (Map.Entry<Long, Double> e : heights.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = e.getKey().intValue();
            double h = e.getValue();
            Cell cell = cells.get(e.getKey());
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                Double n = heights.get(key(x + o[0], z + o[1]));
                if (n != null && n == h + 1) {
                    Block slab = cell.core ? style.coreSlab() : style.shoulderSlab();
                    c.putSoft(x, (int) h, z, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    break;
                }
            }
        }
        return heights;
    }

    /** Rain puddles in flat core stretches: a water block contained by solid road on all four sides. */
    static void puddles(SiteCanvas c, Map<Long, Double> heights, float chance) {
        for (Map.Entry<Long, Double> e : heights.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = e.getKey().intValue();
            double h = e.getValue();
            if (h != Math.floor(h) || c.rnd.nextFloat() >= chance) {
                continue;
            }
            boolean flat = c.get(x, (int) h - 2, z).isSolid();
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                Double n = heights.get(key(x + o[0], z + o[1]));
                if (n == null || n != h || !c.get(x + o[0], (int) h - 1, z + o[1]).isSolid()) {
                    flat = false;
                    break;
                }
            }
            if (flat) {
                c.putSoft(x, (int) h - 1, z, Blocks.WATER.defaultBlockState());
            }
        }
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** Walking y (integer block the player stands in) for a route height in half blocks. */
    static int walkY(double h) {
        return (int) Math.ceil(h);
    }

    static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }
}
