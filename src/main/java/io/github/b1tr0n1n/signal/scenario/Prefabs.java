package io.github.b1tr0n1n.signal.scenario;

import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Reusable structures, each drawn in a local frame whose origin is the walking level at its base. */
final class Prefabs {
    private Prefabs() {
    }

    static BlockState slab(Block b, SlabType t) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, t);
    }

    static BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    // ------------------------------------------------------------------ relay tower

    /** Top deck walking height above the base, and the beacon offset, for bounds and lookups. */
    static final int TOWER_DECK = 29;
    static final int TOWER_TOP = 39;

    /**
     * Lattice relay tower R4: 7x7 lower section to a first platform at +14, 5x5 upper section to a railed
     * deck at +28, mast, canted antenna panel and beacon. A caged ladder climbs a concrete cable riser
     * through hatches in both platforms. {@code base} is the walking level at the tower centre.
     */
    static void tower(SiteCanvas c, BlockPos base) {
        c.frame(base);
        BlockState leg = Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState();
        BlockState bar = Blocks.IRON_BARS.defaultBlockState();
        BlockState grate = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false);
        // Footings.
        for (int[] f : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            c.fill(f[0], -2, f[1], f[0], 0, f[1], Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
        // Lower section: legs at +-3, X-bracing on every face in two 7-high bays.
        for (int y = 1; y <= 13; y++) {
            for (int[] f : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
                c.set(f[0], y, f[1], leg);
            }
        }
        for (int bay = 0; bay < 2; bay++) {
            int y0 = 1 + bay * 7;
            for (int i = 0; i <= 6; i++) {
                int a = -3 + i;
                brace(c, a, y0 + i, 1, 3, bar);
                brace(c, a, y0 + 6 - i, -1, 3, bar);
            }
        }
        // Platform 1 at +14 with railing.
        c.fill(-3, 14, -3, 3, 14, 3, grate);
        for (int a = -3; a <= 3; a++) {
            c.set(a, 15, -3, bar);
            c.set(a, 15, 3, bar);
            c.set(-3, 15, a, bar);
            c.set(3, 15, a, bar);
        }
        // Upper section: legs at +-2, X-bracing in two bays.
        for (int y = 15; y <= 27; y++) {
            for (int[] f : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                c.set(f[0], y, f[1], leg);
            }
        }
        for (int bay = 0; bay < 2; bay++) {
            int y0 = 17 + bay * 5;
            for (int i = 0; i <= 4; i++) {
                int a = -2 + i;
                brace(c, a, y0 + i, 1, 2, bar);
                brace(c, a, y0 + 4 - i, -1, 2, bar);
            }
        }
        // Top deck at +28 with railing (one rail section missing: "don't trust the top rail").
        c.fill(-2, 28, -2, 2, 28, 2, grate);
        for (int a = -2; a <= 2; a++) {
            c.set(a, 29, -2, bar);
            c.set(-2, 29, a, bar);
            c.set(2, 29, a, bar);
            if (a != 1) {
                c.set(a, 29, 2, bar);
            }
        }
        // Cable riser (solid spine) with the ladder on its south face, through both platforms.
        for (int y = 0; y <= 28; y++) {
            c.set(0, y, 0, Blocks.POLISHED_DEEPSLATE);
            c.set(0, y, 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        // Safety cage, open where the ladder passes a platform so the player can step off.
        for (int y = 3; y <= 26; y++) {
            if (y >= 13 && y <= 17) {
                continue;
            }
            c.set(-1, y, 1, bar);
            c.set(1, y, 1, bar);
            c.set(-1, y, 2, bar);
            c.set(0, y, 2, bar);
            c.set(1, y, 2, bar);
        }
        // Mast, antenna panel canted off its bearing, beacon.
        for (int y = 29; y <= 37; y++) {
            c.set(0, y, 0, leg);
        }
        c.set(-1, 32, -1, Blocks.WAXED_WEATHERED_CUT_COPPER);
        c.set(0, 32, -1, Blocks.WAXED_WEATHERED_CUT_COPPER);
        c.set(0, 33, -1, Blocks.WAXED_WEATHERED_CUT_COPPER);
        c.set(1, 31, -1, slab(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, SlabType.TOP));
        c.set(-1, 33, -1, slab(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, SlabType.BOTTOM));
        c.bindLamp(0, 38, 0, BellwetherSite.NODE_RELAY);
        c.set(0, 39, 0, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        // Work light on platform 1 and the bearing mount note on the deck.
        c.set(2, 15, 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        c.standingSign(-1, 29, 1, 8, "R4 ANTENNA", "BEARING: SLIPPED", "ALIGN AT MOUNT", "");
        // Base: cable junction box and warning sign.
        c.set(1, 0, -1, Blocks.WAXED_EXPOSED_COPPER);
        c.standingSign(0, 0, 4, 0, "R4 REGIONAL", "RELAY MAST", "CLIMB AT OWN", "RISK");
    }

    /**
     * One point of an X-brace on all four faces of a square section with legs at +-{@code half}. Iron bars
     * only join side by side, so a pure diagonal renders as loose floating posts; each point also fills the
     * cell beside it ({@code step} = +1 on a rising diagonal, -1 on a falling one), turning the diagonal into
     * a connected staircase that meets the legs at both ends. Mirrored, so both diagonals stay symmetric.
     */
    private static void brace(SiteCanvas c, int a, int y, int step, int half, BlockState bar) {
        for (int x : new int[]{a, a + step}) {
            if (Math.abs(x) < half) {
                c.set(x, y, -half, bar);
                c.set(x, y, half, bar);
                c.set(-half, y, x, bar);
                c.set(half, y, x, bar);
            }
        }
    }

    // ------------------------------------------------------------------ power line

    static void pole(SiteCanvas c, BlockPos base, Direction.Axis wireAxis) {
        c.frame(base);
        for (int y = 0; y <= 6; y++) {
            c.set(0, y, 0, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
        }
        Direction.Axis arm = wireAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockState crossarm = log(Blocks.DARK_OAK_LOG, arm);
        if (arm == Direction.Axis.X) {
            c.set(-1, 6, 0, crossarm);
            c.set(1, 6, 0, crossarm);
        } else {
            c.set(0, 6, -1, crossarm);
            c.set(0, 6, 1, crossarm);
        }
        c.set(0, 7, 0, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
    }

    /** Sagging cable between two attachment points, drawn with chain. */
    static void wire(SiteCanvas c, BlockPos a, BlockPos b, double sag) {
        // Chain is a rod along one axis, so the cable walks a 4-connected path (one axis per step): a
        // diagonal of corner-touching chains would render as loose floating dashes.
        int dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
        int ax = Math.abs(dx), az = Math.abs(dz), sx = Integer.signum(dx), sz = Integer.signum(dz);
        int steps = ax + az;
        int[] cx = new int[steps], cz = new int[steps], cy = new int[steps];
        Direction.Axis[] along = new Direction.Axis[steps];
        int x = a.getX(), z = a.getZ(), ix = 0, iz = 0;
        for (int k = 0; k < steps; k++) {
            boolean stepX = iz >= az || ix < ax && (ix + 0.5) / ax < (iz + 0.5) / az;
            if (stepX) {
                x += sx;
                ix++;
            } else {
                z += sz;
                iz++;
            }
            double t = (k + 1) / (double) steps;
            cx[k] = x;
            cz[k] = z;
            cy[k] = (int) Math.round(a.getY() + (b.getY() - a.getY()) * t - sag * 4 * t * (1 - t));
            along[k] = stepX ? Direction.Axis.X : Direction.Axis.Z;
        }
        // The last cell is the far pole itself.
        for (int k = 0; k < steps - 1; k++) {
            link(c, cx[k], cy[k], cz[k], along[k]);
            // Where the sag drops or climbs a level, hang a vertical link in the lower cell's column.
            int next = k + 1 < steps - 1 ? cy[k + 1] : b.getY();
            if (next != cy[k]) {
                int col = next < cy[k] ? k + 1 : k;
                if (col < steps - 1) {
                    for (int y = Math.min(next, cy[k]) + 1; y <= Math.max(next, cy[k]); y++) {
                        link(c, cx[col], y, cz[col], Direction.Axis.Y);
                    }
                }
            }
        }
    }

    private static void link(SiteCanvas c, int x, int y, int z, Direction.Axis axis) {
        BlockState here = c.get(x, y, z);
        if (here.isAir() || here.is(net.minecraft.tags.BlockTags.LEAVES) || here.is(Blocks.CHAIN)) {
            c.putSoft(x, y, z, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, axis));
        }
    }

    // ------------------------------------------------------------------ rail

    static BlockState rail(RailShape shape) {
        return Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape);
    }

    /** Derelict boxcar standing on a north-south track; {@code base} is the rail level at its south end. */
    static void boxcar(SiteCanvas c, BlockPos base) {
        c.frame(base);
        for (int z = 0; z >= -7; z--) {
            for (int x = -1; x <= 1; x++) {
                c.set(x, 0, z, Blocks.AIR.defaultBlockState());
            }
        }
        for (int[] w : new int[][]{{-1, 0}, {1, 0}, {-1, -7}, {1, -7}}) {
            c.set(w[0], 0, w[1], Blocks.POLISHED_BLACKSTONE_WALL);
        }
        for (int z = 0; z >= -7; z--) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 1; y <= 3; y++) {
                    boolean shellBlock = x != 0 || z == 0 || z == -7 || y == 1;
                    if (!shellBlock) {
                        continue;
                    }
                    // Oxide-red paint long gone to brown, rust breaking through.
                    BlockState s = (z + x) % 2 == 0 ? Blocks.BROWN_TERRACOTTA.defaultBlockState() : Blocks.GRAY_TERRACOTTA.defaultBlockState();
                    if (c.rnd.nextFloat() < 0.15F) {
                        s = Blocks.WAXED_WEATHERED_COPPER.defaultBlockState();
                    }
                    c.set(x, y, z, s);
                }
            }
        }
        // Sliding door left open on the west side, roof.
        c.set(-1, 2, -3, Blocks.AIR.defaultBlockState());
        c.set(-1, 2, -4, Blocks.AIR.defaultBlockState());
        c.set(-1, 3, -3, Blocks.AIR.defaultBlockState());
        c.set(-1, 3, -4, Blocks.AIR.defaultBlockState());
        c.fill(-1, 4, 0, 1, 4, -7, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        c.set(0, 2, -5, Blocks.COBWEB);
        c.set(0, 2, -1, Blocks.BARREL);
    }

    // ------------------------------------------------------------------ vehicles and clutter

    /** Abandoned maintenance truck, nose toward +x. */
    static void truck(SiteCanvas c, BlockPos base) {
        c.frame(base);
        c.set(0, 0, 0, Blocks.POLISHED_BLACKSTONE);
        c.set(0, 0, 2, Blocks.POLISHED_BLACKSTONE);
        c.set(4, 0, 0, Blocks.POLISHED_BLACKSTONE);
        c.set(4, 0, 2, slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM));
        c.fill(0, 1, 0, 4, 1, 2, Blocks.GREEN_TERRACOTTA.defaultBlockState());
        c.set(1, 1, 1, Blocks.BROWN_TERRACOTTA);
        for (int x = 0; x <= 2; x++) {
            c.set(x, 2, 0, Blocks.SPRUCE_FENCE);
            c.set(x, 2, 2, Blocks.SPRUCE_FENCE);
        }
        c.set(3, 2, 0, Blocks.GREEN_TERRACOTTA);
        c.set(3, 2, 2, Blocks.GREEN_TERRACOTTA);
        for (int z = 0; z <= 2; z++) {
            c.set(4, 2, z, Blocks.GLASS_PANE);
            c.set(3, 3, z, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
            c.set(4, 3, z, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        }
        c.set(1, 2, 1, Blocks.BARREL);
    }

    /** Burnt-out car shell on its side of the road. */
    static void carWreck(SiteCanvas c, BlockPos base) {
        c.frame(base);
        c.fill(0, 0, 0, 3, 0, 1, Blocks.BLACKSTONE.defaultBlockState());
        c.set(0, 1, 0, Blocks.COAL_BLOCK);
        c.set(3, 1, 1, Blocks.COAL_BLOCK);
        c.set(1, 1, 0, Blocks.GLASS_PANE);
        c.set(2, 1, 1, Blocks.IRON_BARS);
        c.set(1, 1, 1, slab(Blocks.BLACKSTONE_SLAB, SlabType.BOTTOM));
        c.set(2, 1, 0, slab(Blocks.BLACKSTONE_SLAB, SlabType.BOTTOM));
    }

    /** Rusted shipping container, 8 long on x, doors at +x. */
    static void container(SiteCanvas c, BlockPos base) {
        c.frame(base);
        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 2; z++) {
                    boolean shellBlock = y == 0 || y == 2 || z == 0 || z == 2 || x == 0 || x == 7;
                    if (!shellBlock) {
                        c.set(x, y, z, Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    // Faded teal-grey corrugation with rust breaking through.
                    BlockState s = x % 2 == 0 ? Blocks.CYAN_TERRACOTTA.defaultBlockState() : Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
                    if (c.rnd.nextFloat() < 0.14F) {
                        s = Blocks.BROWN_TERRACOTTA.defaultBlockState();
                    }
                    c.set(x, y, z, s);
                }
            }
        }
        c.set(7, 1, 1, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        c.set(3, 1, 1, Blocks.COBWEB);
    }

    /** Collapsed timber shed: three standing walls, one fallen flat, roof half caved in. */
    static void collapsedShed(SiteCanvas c, BlockPos base) {
        c.frame(base);
        for (int y = 0; y <= 2; y++) {
            for (int a = 0; a <= 5; a++) {
                c.set(a, y, 0, y == 2 && a > 3 ? Blocks.AIR.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                c.set(0, y, a, Blocks.SPRUCE_PLANKS);
                if (y < 2 || a < 2) {
                    c.set(5, y, a, Blocks.SPRUCE_PLANKS);
                }
            }
        }
        for (int[] p : new int[][]{{0, 0}, {5, 0}, {0, 5}, {5, 5}}) {
            for (int y = 0; y <= 2; y++) {
                c.set(p[0], y, p[1], log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
            }
        }
        // Fallen south wall lying in the mud.
        for (int a = 1; a <= 4; a++) {
            c.set(a, 0, 7, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
            c.set(a, 0, 8, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
        // Roof: intact at the back, sagging toward the front.
        for (int a = 0; a <= 5; a++) {
            c.set(a, 3, 0, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
            c.set(a, 3, 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
            if (a < 3) {
                c.set(a, 2, 3, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
                c.set(a, 1, 4, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
            }
        }
        c.set(2, 0, 2, Blocks.COBWEB);
        c.set(4, 0, 1, Blocks.BARREL);
        c.set(3, 0, 1, Blocks.CAULDRON);
    }

    /** Checkpoint booth by the gate: 3x3, window to the road, slab roof. */
    static void booth(SiteCanvas c, BlockPos base, Direction window) {
        c.frame(base);
        for (int y = 0; y <= 2; y++) {
            for (int a = 0; a <= 2; a++) {
                c.set(a, y, 0, Blocks.TUFF);
                c.set(a, y, 2, Blocks.TUFF);
                c.set(0, y, a, Blocks.TUFF);
                c.set(2, y, a, Blocks.TUFF);
            }
        }
        c.set(1, 0, 1, Blocks.AIR.defaultBlockState());
        c.set(1, 1, 1, Blocks.AIR.defaultBlockState());
        c.set(1, 2, 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        c.fill(-1, 3, -1, 3, 3, 3, slab(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, SlabType.BOTTOM));
        switch (window) {
            case WEST -> {
                c.set(0, 1, 1, Blocks.GLASS_PANE);
                c.set(2, 0, 1, Blocks.AIR.defaultBlockState());
                c.set(2, 1, 1, Blocks.AIR.defaultBlockState());
            }
            default -> {
                c.set(1, 1, 0, Blocks.GLASS_PANE);
                c.set(1, 0, 2, Blocks.AIR.defaultBlockState());
                c.set(1, 1, 2, Blocks.AIR.defaultBlockState());
            }
        }
    }

    static void lampPost(SiteCanvas c, BlockPos base) {
        c.frame(base);
        c.set(0, 0, 0, Blocks.POLISHED_BLACKSTONE_WALL);
        c.set(0, 1, 0, Blocks.POLISHED_BLACKSTONE_WALL);
        c.set(0, 2, 0, Blocks.POLISHED_BLACKSTONE_WALL);
        c.set(0, 3, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    /** Fire barrel with log benches: the one warm spot in the yard. */
    static void fireBarrel(SiteCanvas c, BlockPos base) {
        c.frame(base);
        c.set(0, 0, 0, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        c.set(-2, 0, 0, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        c.set(2, 0, -1, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        c.set(2, 0, 0, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
        c.set(0, 0, 2, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
    }

    // ------------------------------------------------------------------ Bellwether houses

    /** Small inhabited house, 7 (x) by 6 (z), door on the south, warm light in the windows, chimney smoke. */
    static void house(SiteCanvas c, BlockPos base) {
        c.frame(base);
        c.fill(0, -1, 0, 6, -1, 5, Blocks.STONE_BRICKS.defaultBlockState());
        for (int y = 0; y <= 3; y++) {
            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 5; z++) {
                    boolean edge = x == 0 || x == 6 || z == 0 || z == 5;
                    boolean corner = (x == 0 || x == 6) && (z == 0 || z == 5);
                    if (!edge) {
                        c.set(x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (corner) {
                        c.set(x, y, z, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
                    } else {
                        c.set(x, y, z, y == 0 ? Blocks.MUD_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                    }
                }
            }
        }
        // Windows with warm light behind.
        for (int[] w : new int[][]{{2, 5}, {4, 5}, {0, 2}, {6, 3}}) {
            c.set(w[0], 2, w[1], Blocks.GLASS_PANE);
        }
        c.set(2, 3, 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        c.set(4, 3, 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Door.
        c.set(3, 0, 5, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        c.set(3, 1, 5, Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // Gable roof running along x.
        for (int x = -1; x <= 7; x++) {
            for (int k = 0; k <= 3; k++) {
                int y = 4 + k;
                BlockState north = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
                BlockState south = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
                if (k < 3) {
                    c.set(x, y, -1 + k, north);
                    c.set(x, y, 6 - k, south);
                } else {
                    c.set(x, y - 1, 2, Blocks.DARK_OAK_PLANKS);
                    c.set(x, y - 1, 3, Blocks.DARK_OAK_PLANKS);
                }
            }
        }
        for (int z = 1; z <= 4; z++) {
            for (int y = 4; y <= 5; y++) {
                c.set(0, y, z, Blocks.SPRUCE_PLANKS);
                c.set(6, y, z, Blocks.SPRUCE_PLANKS);
            }
        }
        // Chimney with a lit campfire on top: a thin line of smoke you can see from the tower.
        for (int y = 0; y <= 7; y++) {
            c.set(5, y, 1, Blocks.COBBLESTONE);
        }
        c.set(5, 8, 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        // Inside: table, stool, bed roll, a kettle on the stove.
        c.set(2, 0, 2, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        c.set(4, 0, 1, Blocks.SMOKER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        c.set(1, 0, 4, Blocks.BROWN_CARPET);
        c.set(2, 0, 4, Blocks.BROWN_CARPET);
    }
}
