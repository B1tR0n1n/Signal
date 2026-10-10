package io.github.b1tr0n1n.signal.scenario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;

/**
 * Survey of the natural ground before construction: for each column, the y of the topmost block that is
 * actually ground (trees, plants, snow, leaves and water are looked through). Taken once, before any
 * writes, so later sculpting can blend back to the original slope.
 */
public final class Terrain {
    final int x0;
    final int z0;
    final int width;
    final int depth;
    private final int[] ground;
    private final int[] water;
    private final boolean[] wet;

    private Terrain(int x0, int z0, int width, int depth) {
        this.x0 = x0;
        this.z0 = z0;
        this.width = width;
        this.depth = depth;
        this.ground = new int[width * depth];
        this.water = new int[width * depth];
        this.wet = new boolean[width * depth];
    }

    static Terrain survey(ServerLevel level, int x0, int z0, int width, int depth) {
        Terrain t = new Terrain(x0, z0, width, depth);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int floor = level.getMinBuildHeight();
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                boolean water = false;
                int surface = Integer.MIN_VALUE;
                while (y > floor) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (!s.getFluidState().isEmpty()) {
                        water = true;
                        surface = Math.max(surface, y);
                    }
                    if (!isCover(s)) {
                        break;
                    }
                    y--;
                }
                t.ground[dx * depth + dz] = y;
                t.water[dx * depth + dz] = water ? surface : y;
                t.wet[dx * depth + dz] = water;
            }
        }
        return t;
    }

    /** Things standing on the ground rather than being the ground. */
    static boolean isCover(BlockState s) {
        return s.isAir()
                || s.is(Blocks.BARRIER) // never natural; GameTest volumes are roofed with it
                || !s.getFluidState().isEmpty()
                || s.is(BlockTags.LOGS)
                || s.is(BlockTags.LEAVES)
                || s.is(BlockTags.REPLACEABLE)
                || s.canBeReplaced()
                || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.MOSS_CARPET)
                || s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.MUSHROOM_STEM)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.BAMBOO)
                || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(Blocks.LILY_PAD) || s.is(Blocks.SNOW);
    }

    boolean contains(int x, int z) {
        return x >= x0 && x < x0 + width && z >= z0 && z < z0 + depth;
    }

    /** Original ground y at (x, z); clamps to the survey edge. */
    int at(int x, int z) {
        int dx = Math.max(0, Math.min(width - 1, x - x0));
        int dz = Math.max(0, Math.min(depth - 1, z - z0));
        return ground[dx * depth + dz];
    }

    /**
     * Where something built here should stand: the water surface over a lake or river, the ground elsewhere.
     * Routes and pads use this, so a road crosses water as a causeway instead of a trench through the lake.
     */
    int level(int x, int z) {
        int dx = Math.max(0, Math.min(width - 1, x - x0));
        int dz = Math.max(0, Math.min(depth - 1, z - z0));
        return water[dx * depth + dz];
    }

    boolean wetAt(int x, int z) {
        int dx = Math.max(0, Math.min(width - 1, x - x0));
        int dz = Math.max(0, Math.min(depth - 1, z - z0));
        return wet[dx * depth + dz];
    }

    int min() {
        return Arrays.stream(ground).min().orElse(0);
    }

    int max() {
        return Arrays.stream(ground).max().orElse(0);
    }

    int maxIn(int ax, int az, int bx, int bz) {
        int m = Integer.MIN_VALUE;
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
            for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
                m = Math.max(m, at(x, z));
            }
        }
        return m;
    }

    int median(int ax, int az, int bx, int bz) {
        int n = (Math.abs(bx - ax) + 1) * (Math.abs(bz - az) + 1);
        int[] v = new int[n];
        int i = 0;
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
            for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
                v[i++] = level(x, z);
            }
        }
        Arrays.sort(v);
        return v[n / 2];
    }

    // ------------------------------------------------------------------ sculpting

    /**
     * Set column (x, z) so its top solid block is at {@code surfaceY}: fill below with soil (stone deeper),
     * cut everything above, and clear cover (plants, logs, leaves) up to {@code clearTo}.
     */
    void shape(SiteCanvas c, int x, int z, int surfaceY, BlockState top, int clearTo) {
        int g = at(x, z);
        // Fill: soil near the surface, stone deeper. (A cut needs no fill.)
        for (int y = g + 1; y < surfaceY; y++) {
            c.putSoft(x, y, z, (y < surfaceY - 3 ? Blocks.STONE : Blocks.DIRT).defaultBlockState());
        }
        c.putSoft(x, surfaceY, z, top);
        // Cut and clear: everything above the new surface, at least up to the old ground.
        int cleared = Math.max(clearTo, g);
        for (int y = surfaceY + 1; y <= cleared; y++) {
            c.clear(x, y, z);
        }
        // Nothing may be left hanging over the cut: a tree whose trunk was just cleared (or anything natural
        // resting on it) would float. Clear the rest of the column up to the first air.
        for (int y = cleared + 1; y <= c.bounds.maxY(); y++) {
            BlockState s = c.get(x, y, z);
            if (s.isAir() || !s.getFluidState().isEmpty() || !(isCover(s) || ScenarioGuard.isNatural(s))) {
                break;
            }
            c.clear(x, y, z);
        }
    }

    /** Smoothstep blend factor for a column at distance {@code d} (1..margin) outside a pad. */
    static double blend(int d, int margin) {
        double t = Math.min(1.0, d / (double) (margin + 1));
        return t * t * (3 - 2 * t);
    }

    /**
     * Level a rectangular pad at {@code padY} (top block y) and blend a {@code margin}-wide apron back to
     * the surveyed slope. Cover is cleared over the pad up to {@code padY + clearHeight}.
     */
    interface Surface {
        BlockState at(int x, int z);
    }

    void pad(SiteCanvas c, int ax, int az, int bx, int bz, int padY, int margin, int clearHeight, Surface padTop) {
        int minX = Math.min(ax, bx), maxX = Math.max(ax, bx), minZ = Math.min(az, bz), maxZ = Math.max(az, bz);
        for (int x = minX - margin; x <= maxX + margin; x++) {
            for (int z = minZ - margin; z <= maxZ + margin; z++) {
                int dx = x < minX ? minX - x : x > maxX ? x - maxX : 0;
                int dz = z < minZ ? minZ - z : z > maxZ ? z - maxZ : 0;
                int d = Math.max(dx, dz);
                if (d == 0) {
                    shape(c, x, z, padY, padTop.at(x, z), padY + clearHeight);
                } else {
                    int g = level(x, z);
                    int target = (int) Math.round(padY + (g - padY) * blend(d, margin));
                    if (target != g && !(wetAt(x, z) && target <= g)) { // never dig into standing water
                        BlockState top = c.noise(x, z, 4.0) > 0.25 ? Blocks.COARSE_DIRT.defaultBlockState()
                                : Blocks.GRASS_BLOCK.defaultBlockState();
                        shape(c, x, z, target, top, Math.max(target, g) + clearHeight / 2);
                    }
                }
            }
        }
    }
}
