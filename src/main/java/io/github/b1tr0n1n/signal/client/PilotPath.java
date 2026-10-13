package io.github.b1tr0n1n.signal.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Walking routes for the playtest pilot: A* over the blocks the client has loaded. A node is the block a
 * player's feet occupy. Moves go to the 8 neighbours, up at most one block (with a jump) and down at most
 * three, never through fences, walls or bars (their collision is taller than a step), never cutting a corner,
 * avoiding lava and preferring dry ground to water.
 */
final class PilotPath {
    private static final int MAX_NODES = 40_000;
    private static final int MAX_DROP = 3;
    private static final double STEP = 0.6;

    private PilotPath() {
    }

    /** Waypoints (block-centre, standing height) from {@code from} to within 1.5 blocks of (tx, tz), or empty. */
    static List<Vec3> find(BlockGetter level, Vec3 from, double tx, double tz) {
        BlockPos start = standingCell(level, BlockPos.containing(from.x, from.y + 0.01, from.z));
        if (start == null) {
            return List.of();
        }
        record Node(BlockPos pos, double g, double f) {
        }
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f(), b.f()));
        Map<BlockPos, Double> best = new HashMap<>();
        Map<BlockPos, BlockPos> came = new HashMap<>();
        open.add(new Node(start, 0, h(start, tx, tz)));
        best.put(start, 0.0);
        int expanded = 0;
        while (!open.isEmpty() && expanded++ < MAX_NODES) {
            Node n = open.poll();
            if (n.g() > best.getOrDefault(n.pos(), Double.MAX_VALUE)) {
                continue;
            }
            if (Math.hypot(n.pos().getX() + 0.5 - tx, n.pos().getZ() + 0.5 - tz) <= 1.5) {
                return waypoints(level, came, n.pos());
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos next = step(level, n.pos(), dx, dz);
                    if (next == null) {
                        continue;
                    }
                    double cost = (dx != 0 && dz != 0 ? 1.414 : 1.0) + (next.getY() > n.pos().getY() ? 0.6 : 0)
                            + (n.pos().getY() - next.getY() > 1 ? 0.4 : 0)
                            + (level.getFluidState(next).is(FluidTags.WATER) ? 3 : 0);
                    double g = n.g() + cost;
                    if (g < best.getOrDefault(next, Double.MAX_VALUE)) {
                        best.put(next, g);
                        came.put(next, n.pos());
                        open.add(new Node(next, g, g + h(next, tx, tz)));
                    }
                }
            }
        }
        return List.of();
    }

    /** What the planner sees around a position: for each cell from y-2 to y+1, block, clear, standable. */
    static String probe(BlockGetter level, Vec3 at) {
        BlockPos base = BlockPos.containing(at.x, at.y + 0.01, at.z);
        StringBuilder b = new StringBuilder("start=").append(standingCell(level, base));
        for (int dy = -2; dy <= 1; dy++) {
            BlockPos p = base.above(dy);
            VoxelShape s = shape(level, p);
            b.append(" | y").append(p.getY()).append(' ').append(level.getBlockState(p).getBlock().getDescriptionId())
                    .append(" max=").append(s.isEmpty() ? "-" : String.format("%.2f", s.max(Direction.Axis.Y)))
                    .append(" clear=").append(clear(level, p)).append(" stand=").append(standable(level, p));
        }
        return b.toString();
    }

    private static double h(BlockPos p, double tx, double tz) {
        double dx = Math.abs(p.getX() + 0.5 - tx);
        double dz = Math.abs(p.getZ() + 0.5 - tz);
        return Math.max(dx, dz) + 0.414 * Math.min(dx, dz);
    }

    private static List<Vec3> waypoints(BlockGetter level, Map<BlockPos, BlockPos> came, BlockPos end) {
        List<Vec3> out = new ArrayList<>();
        for (BlockPos p = end; p != null; p = came.get(p)) {
            out.add(new Vec3(p.getX() + 0.5, standHeight(level, p), p.getZ() + 0.5));
        }
        Collections.reverse(out);
        return out;
    }

    /** The cell reached by moving (dx, dz) from {@code from}, at whatever height the ground there allows. */
    private static BlockPos step(BlockGetter level, BlockPos from, int dx, int dz) {
        if (dx != 0 && dz != 0) {
            // No corner cutting: both orthogonal cells must be open at our height.
            if (!clear(level, from.offset(dx, 0, 0)) || !clear(level, from.offset(0, 0, dz))) {
                return null;
            }
        }
        double here = standHeight(level, from);
        BlockPos col = from.offset(dx, 0, dz);
        for (int dy = 1; dy >= -MAX_DROP; dy--) {
            BlockPos c = col.above(dy);
            if (!standable(level, c)) {
                continue;
            }
            double rise = standHeight(level, c) - here;
            if (rise > 1.05) {
                return null;
            }
            if (rise > STEP && !clear(level, from.above(1))) {
                return null; // no headroom to jump
            }
            // Walking off an edge needs the column above the landing open up to our head.
            for (int y = c.getY() + 2; y <= from.getY() + 1; y++) {
                if (!passable(level, new BlockPos(c.getX(), y, c.getZ()))) {
                    return null;
                }
            }
            return c;
        }
        return null;
    }

    /**
     * The cell to plan from: the feet cell if it is standable, else the nearest standable neighbour. A player
     * can stand in part of a cell the planner treats as blocked (beside the thin bar of a pane or fence).
     */
    private static BlockPos standingCell(BlockGetter level, BlockPos p) {
        BlockPos found = null;
        int bestScore = Integer.MAX_VALUE;
        for (int r = 0; r <= 1 && found == null; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = 1; dy >= -2; dy--) {
                        BlockPos c = p.offset(dx, dy, dz);
                        int score = Math.abs(dx) + Math.abs(dz) + Math.abs(dy);
                        if (score < bestScore && standable(level, c)) {
                            found = c;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        return found;
    }

    /** Feet and head fit here and there is something to stand on. */
    private static boolean standable(BlockGetter level, BlockPos feet) {
        if (!clear(level, feet) || level.getFluidState(feet).is(FluidTags.LAVA)) {
            return false;
        }
        VoxelShape own = shape(level, feet);
        if (!own.isEmpty()) {
            return true; // a slab, path or carpet is its own floor
        }
        VoxelShape below = shape(level, feet.below());
        BlockState under = level.getBlockState(feet.below());
        return !below.isEmpty() && below.max(Direction.Axis.Y) <= 1.0 && wide(below) && !level.getFluidState(feet.below()).is(FluidTags.LAVA)
                && !under.isAir() || level.getFluidState(feet).is(FluidTags.WATER);
    }

    /** Something you can actually stand on: not the thin top of bars, a pane or a chain. */
    private static boolean wide(VoxelShape s) {
        var box = s.bounds();
        return box.maxX - box.minX >= 0.5 && box.maxZ - box.minZ >= 0.5;
    }

    /** Feet cell low enough to stand in (empty, or a floor no taller than a step) and head cell empty. */
    private static boolean clear(BlockGetter level, BlockPos feet) {
        VoxelShape own = shape(level, feet);
        return (own.isEmpty() || own.max(Direction.Axis.Y) <= 0.5) && passable(level, feet.above());
    }

    private static boolean passable(BlockGetter level, BlockPos p) {
        return shape(level, p).isEmpty();
    }

    private static double standHeight(BlockGetter level, BlockPos feet) {
        VoxelShape own = shape(level, feet);
        return feet.getY() + (own.isEmpty() ? 0 : own.max(Direction.Axis.Y));
    }

    private static VoxelShape shape(BlockGetter level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p);
    }
}
