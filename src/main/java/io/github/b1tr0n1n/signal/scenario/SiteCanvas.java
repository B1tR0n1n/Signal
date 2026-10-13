package io.github.b1tr0n1n.signal.scenario;

import io.github.b1tr0n1n.signal.block.SiteMachineBlock;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import io.github.b1tr0n1n.signal.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Write-guarded drawing surface for scenario construction. Every write must fall inside the declared
 * bounds (enforced, not assumed). Writes use known-shape updates so nothing cascades outside the site;
 * connection-sensitive blocks are re-shaped in one final pass. Unchanged blocks are not rewritten.
 */
final class SiteCanvas {
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    final ServerLevel level;
    final BoundingBox bounds;
    final RandomSource rnd;
    private final List<BlockPos> needsShape = new ArrayList<>();
    final Map<BlockPos, String> bindings = new LinkedHashMap<>();

    /** Current local frame: {@link #rel} and the frame-relative helpers are offsets from this origin. */
    private BlockPos frame = BlockPos.ZERO;

    private final SimplexNoise noise;

    SiteCanvas(ServerLevel level, BoundingBox bounds, long seed) {
        this.level = level;
        this.bounds = bounds;
        this.rnd = RandomSource.create(seed);
        this.noise = new SimplexNoise(RandomSource.create(seed ^ 0x9E3779B97F4A7C15L));
    }

    /** Smooth 2D noise in [-1, 1] at the given feature scale (blocks), for coherent surface patches. */
    double noise(int x, int z, double scale) {
        return noise.getValue(x / scale, z / scale);
    }

    void frame(BlockPos origin) {
        this.frame = origin.immutable();
    }

    BlockPos rel(int dx, int dy, int dz) {
        return frame.offset(dx, dy, dz);
    }

    // ----- absolute writes

    void put(BlockPos p, BlockState s) {
        if (!bounds.isInside(p)) {
            throw new IllegalStateException("Scenario write outside bounds: " + p + " not in " + bounds);
        }
        if (level.getBlockState(p) != s) {
            level.setBlock(p, s, FLAGS);
        }
        Block b = s.getBlock();
        if (b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock) {
            needsShape.add(p.immutable());
        }
    }

    void put(int x, int y, int z, BlockState s) {
        put(new BlockPos(x, y, z), s);
    }

    void put(int x, int y, int z, Block b) {
        put(new BlockPos(x, y, z), b.defaultBlockState());
    }

    /** Like {@link #put} but silently skips positions outside the bounds (for organic edges). */
    void putSoft(int x, int y, int z, BlockState s) {
        BlockPos p = new BlockPos(x, y, z);
        if (bounds.isInside(p)) {
            put(p, s);
        }
    }

    BlockState get(int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z));
    }

    void clear(int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        BlockState old = level.getBlockState(p);
        if (bounds.isInside(p) && !old.isAir()) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
            if (old.is(BlockTags.LOGS)) {
                // Known-shape writes skip neighbour updates, so leaves would keep a stale distance to this log
                // and never decay. Re-check the touching leaves; the cascade reaches the rest of the crown.
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    BlockState leaf = level.getBlockState(n);
                    if (leaf.getBlock() instanceof LeavesBlock) {
                        level.scheduleTick(n, leaf.getBlock(), 1);
                    }
                }
            }
        }
    }

    // ----- frame-relative writes (used by the facility, tower and prefabs)

    void set(int dx, int dy, int dz, BlockState s) {
        put(rel(dx, dy, dz), s);
    }

    void set(int dx, int dy, int dz, Block b) {
        set(dx, dy, dz, b.defaultBlockState());
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, s);
                }
            }
        }
    }

    BlockState pick(Block a, float pa, Block b, float pb, Block c) {
        float r = rnd.nextFloat();
        if (r < pa) {
            return a.defaultBlockState();
        }
        if (r < pa + pb) {
            return b.defaultBlockState();
        }
        return c.defaultBlockState();
    }

    BlockState weighted(Object... blocksAndWeights) {
        float total = 0;
        for (int i = 1; i < blocksAndWeights.length; i += 2) {
            total += ((Number) blocksAndWeights[i]).floatValue();
        }
        float r = rnd.nextFloat() * total;
        for (int i = 0; i < blocksAndWeights.length; i += 2) {
            r -= ((Number) blocksAndWeights[i + 1]).floatValue();
            if (r <= 0) {
                return ((Block) blocksAndWeights[i]).defaultBlockState();
            }
        }
        return ((Block) blocksAndWeights[blocksAndWeights.length - 2]).defaultBlockState();
    }

    // ----- equipment and bindings

    void bindMachine(int dx, int dy, int dz, Block block, Direction facing, String node) {
        set(dx, dy, dz, block.defaultBlockState().setValue(SiteMachineBlock.FACING, facing));
        bind(rel(dx, dy, dz), node);
    }

    void bindLamp(int dx, int dy, int dz, String node) {
        set(dx, dy, dz, ModBlocks.SERVICE_LAMP.get().defaultBlockState());
        bind(rel(dx, dy, dz), node);
    }

    void bind(BlockPos p, String node) {
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof SiteNodeBlockEntity site) {
            site.setNodeId(node);
        }
        bindings.put(p.immutable(), node);
    }

    // ----- signs

    private static SignText text(String... lines) {
        SignText t = new SignText();
        for (int i = 0; i < lines.length && i < 4; i++) {
            t = t.setMessage(i, Component.literal(lines[i]));
        }
        return t;
    }

    void writeSign(BlockPos p, String... lines) {
        if (level.getBlockEntity(p) instanceof SignBlockEntity sign) {
            sign.setText(text(lines), true);
            sign.setWaxed(true);
        }
    }

    void standingSign(int dx, int dy, int dz, int rotation, String... lines) {
        set(dx, dy, dz, Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        writeSign(rel(dx, dy, dz), lines);
    }

    /** Wall sign occupying (dx,dy,dz), attached to the block behind it, readable from {@code facing}. */
    void wallSign(int dx, int dy, int dz, Direction facing, String... lines) {
        set(dx, dy, dz, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        writeSign(rel(dx, dy, dz), lines);
    }

    /**
     * Final pass: known-shape writes never wake fluids, so water beside a cut would hang as a sheer wall.
     * Every air cell inside the site that touches water or lava from the side or from above gets a mud bank,
     * which holds the water where it is. (Puddles are already fully contained and are left alone.)
     */
    int containFluids() {
        BlockState bank = Blocks.MUD.defaultBlockState();
        Direction[] spill = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
        List<BlockPos> banks = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    if (level.getFluidState(p.set(x, y, z)).isEmpty()) {
                        continue;
                    }
                    for (Direction d : spill) {
                        BlockPos n = p.relative(d);
                        if (bounds.isInside(n) && level.getBlockState(n).isAir()) {
                            banks.add(n);
                        }
                    }
                }
            }
        }
        for (BlockPos n : banks) {
            put(n, bank);
        }
        return banks.size();
    }

    /**
     * Final pass: tree crowns whose trunk was cut must not hang in the air. Every connected clump of leaves
     * inside the site that touches no log at all is removed. (Leaf decay would not do it: a clump within six
     * blocks of another tree's log counts as supported and stays forever. Seen in the playtest.)
     *
     * @return leaves removed
     */
    int removeOrphanLeaves() {
        java.util.Set<BlockPos> leaves = new java.util.HashSet<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    if (level.getBlockState(p.set(x, y, z)).getBlock() instanceof LeavesBlock) {
                        leaves.add(p.immutable());
                    }
                }
            }
        }
        int removed = 0;
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (BlockPos start : leaves) {
            if (!seen.add(start)) {
                continue;
            }
            List<BlockPos> clump = new ArrayList<>();
            java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(List.of(start));
            boolean held = false;
            while (!queue.isEmpty()) {
                BlockPos q = queue.poll();
                clump.add(q);
                for (Direction d : Direction.values()) {
                    BlockPos n = q.relative(d);
                    if (leaves.contains(n)) {
                        if (seen.add(n)) {
                            queue.add(n);
                        }
                    } else if (!bounds.isInside(n) || level.getBlockState(n).is(BlockTags.LOGS)) {
                        held = true; // touches a log, or runs out of the site where we cannot tell
                    }
                }
            }
            if (!held) {
                for (BlockPos q : clump) {
                    clear(q.getX(), q.getY(), q.getZ());
                }
                removed += clump.size();
            }
        }
        return removed;
    }

    /** Final pass: connect bars, panes, fences, walls and stairs to their neighbours. */
    void finishShapes() {
        for (BlockPos p : needsShape) {
            BlockState s = level.getBlockState(p);
            BlockState updated = Block.updateFromNeighbourShapes(s, level, p);
            if (updated != s) {
                level.setBlock(p, updated, FLAGS);
            }
        }
        needsShape.clear();
    }
}
