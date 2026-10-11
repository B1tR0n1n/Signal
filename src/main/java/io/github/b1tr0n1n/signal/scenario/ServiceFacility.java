package io.github.b1tr0n1n.signal.scenario;

import io.github.b1tr0n1n.signal.campaign.SiteProjector;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.registry.ModBlocks;
import io.github.b1tr0n1n.signal.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bellwether service room and pump stand, drawn in a "facility frame": offsets are the milestone-one
 * layout (walking level dy 0, building x 22..34, z 4..14, pump stand x 35..39). The frame origin is
 * placed on the compound pad by {@link BellwetherScenario}. Equipment, bindings and the interior are
 * unchanged from the tested layout; the shell is weathered concrete.
 */
public final class ServiceFacility {
    /** Frame-relative footprint including roof overhang and pump stand, for clearing and reset. */
    static final int MIN_X = 21, MAX_X = 39, MIN_Z = 3, MAX_Z = 15, MIN_Y = -1, MAX_Y = 7;

    /** Panel position relative to the frame; the spare-fuse cabinet sits at panel + (-4, -1, +3). */
    public static final BlockPos PANEL = new BlockPos(27, 1, 5);
    public static final BlockPos CABINET = new BlockPos(23, 0, 8);
    public static final BlockPos DOOR = new BlockPos(27, 0, 14);

    private ServiceFacility() {
    }

    public static BoundingBox box(BlockPos frame) {
        return new BoundingBox(frame.getX() + MIN_X, frame.getY() + MIN_Y, frame.getZ() + MIN_Z,
                frame.getX() + MAX_X, frame.getY() + MAX_Y, frame.getZ() + MAX_Z);
    }

    static BlockState slab(net.minecraft.world.level.block.Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    static void build(SiteCanvas c, BlockPos frame) {
        c.frame(frame);
        // Clear the footprint first so a reset always rebuilds from nothing.
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                for (int y = 0; y <= MAX_Y; y++) {
                    BlockPos p = c.rel(x, y, z);
                    c.clear(p.getX(), p.getY(), p.getZ());
                }
            }
        }
        shell(c);
        interior(c);
        pumpStand(c);
    }

    private static BlockState concrete(SiteCanvas c, int y) {
        if (y == 0) {
            return c.weighted(Blocks.MOSSY_STONE_BRICKS, 3, Blocks.TUFF, 3, Blocks.MUD_BRICKS, 1);
        }
        return c.weighted(Blocks.TUFF, 4, Blocks.ANDESITE, 3, Blocks.POLISHED_TUFF, 2, Blocks.STONE, 2,
                Blocks.CRACKED_STONE_BRICKS, 1, Blocks.POLISHED_ANDESITE, 1);
    }

    private static void shell(SiteCanvas c) {
        int x0 = 22, x1 = 34, z0 = 4, z1 = 14;
        c.fill(x0, -1, z0, x1, -1, z1, Blocks.STONE_BRICKS.defaultBlockState());
        c.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
        for (int y = 0; y <= 4; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    if (edge) {
                        c.set(x, y, z, concrete(c, y));
                    }
                }
            }
        }
        // Corner pilasters and a lintel band: Soviet-era panel building.
        for (int y = 0; y <= 4; y++) {
            c.set(x0, y, z0, Blocks.POLISHED_ANDESITE);
            c.set(x1, y, z0, Blocks.POLISHED_ANDESITE);
            c.set(x0, y, z1, Blocks.POLISHED_ANDESITE);
            c.set(x1, y, z1, Blocks.POLISHED_ANDESITE);
        }
        for (int x = x0; x <= x1; x++) {
            c.set(x, 4, z1, Blocks.TUFF_BRICKS);
            c.set(x, 4, z0, Blocks.TUFF_BRICKS);
        }
        // Rust and water streaks running down from the roof edge.
        for (int x : new int[]{24, 30, 33}) {
            c.set(x, 3, z1, Blocks.BROWN_TERRACOTTA);
            c.set(x, 2, z1, c.rnd.nextBoolean() ? Blocks.BROWN_TERRACOTTA.defaultBlockState() : Blocks.GRAY_TERRACOTTA.defaultBlockState());
        }
        // Flat roof: smooth stone with a weathered copper parapet, moss where water sits.
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean rim = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
                // Tar roof (blackstone) with a weathered sheet-metal parapet.
                c.set(x, 5, z, slab(rim ? Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB : Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM));
            }
        }
        for (int i = 0; i < 14; i++) {
            int x = x0 + 1 + c.rnd.nextInt(x1 - x0 - 1), z = z0 + 1 + c.rnd.nextInt(z1 - z0 - 1);
            // Moss where water pools on the tar, and patched sections.
            c.set(x, 5, z, slab(c.rnd.nextBoolean() ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.BLACKSTONE_SLAB, SlabType.BOTTOM));
        }
        // Roof plant: a rusted vent box and the generator exhaust.
        c.set(30, 6, 7, Blocks.WAXED_WEATHERED_COPPER);
        c.set(31, 6, 7, Blocks.IRON_TRAPDOOR);
        c.fill(23, 0, 5, 23, 4, 5, Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        c.set(23, 5, 5, Blocks.IRON_BARS);
        c.set(23, 6, 5, Blocks.IRON_BARS);
        c.set(23, 7, 5, Blocks.IRON_BARS);

        // Doorway (south), open: the yard leads straight in. Awning slab over it.
        c.fill(27, 0, z1, 28, 2, z1, Blocks.AIR.defaultBlockState());
        c.set(26, 3, z1 + 1, slab(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, SlabType.TOP));
        c.set(29, 3, z1 + 1, slab(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, SlabType.TOP));
        c.wallSign(27, 3, z1 + 1, Direction.SOUTH, "BELLWETHER", "RELAY STN. 4", "SERVICE ROOM", "");
        c.set(28, 3, z1 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));

        // Windows: grimy panes, one boarded, one smashed open with bars.
        for (int z = 7; z <= 11; z++) {
            c.set(x0, 2, z, z == 9 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.GLASS_PANE.defaultBlockState());
        }
        for (int z = 10; z <= 12; z++) {
            c.set(x1, 2, z, Blocks.GLASS_PANE);
        }
        c.set(24, 2, z1, Blocks.GLASS_PANE);
        c.set(25, 2, z1, Blocks.GLASS_PANE);
        c.set(31, 2, z1, Blocks.GLASS_PANE);
        c.set(32, 2, z1, Blocks.IRON_BARS);

        // Downpipes off the parapet at the two front corners, one emptying into a rain butt.
        for (int x : new int[]{x0 + 1, x1 - 1}) {
            for (int y = 0; y <= 4; y++) {
                c.set(x, y, z1 + 1, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
            }
        }
        c.set(x0 + 1, 0, z1 + 1, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 2));
        // A dark band of damp along the foot of the walls.
        for (int x = x0 + 1; x < x1; x++) {
            if (x != 27 && x != 28 && c.rnd.nextFloat() < 0.5F) { // not in the doorway
                c.set(x, 0, z1, Blocks.MOSSY_STONE_BRICKS);
            }
        }

        // Overgrowth: vines on the north wall, ferns along the damp base.
        for (int x = x0 + 1; x < x1; x++) {
            if (c.rnd.nextFloat() < 0.45F) {
                int h = 1 + c.rnd.nextInt(4);
                for (int y = 4; y > 4 - h; y--) {
                    c.set(x, y, z0 - 1, Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true));
                }
            }
        }
    }

    private static void interior(SiteCanvas c) {
        // North equipment wall, everything facing south toward the room.
        c.bindMachine(24, 0, 5, ModBlocks.GENERATOR.get(), Direction.SOUTH, BellwetherSite.NODE_GENERATOR);
        c.bindLamp(24, 1, 5, BellwetherSite.NODE_GENERATOR);
        c.wallSign(25, 1, 5, Direction.SOUTH, "GENERATOR G1", "RATED 6 UNITS", "USE: CLOSE ISOL.", "SNEAK: ISOLATE");

        c.set(27, 0, 5, Blocks.SMOOTH_STONE);
        c.bindMachine(27, 1, 5, ModBlocks.DISTRIBUTION_PANEL.get(), Direction.SOUTH, BellwetherSite.NODE_PANEL);
        c.wallSign(28, 1, 5, Direction.SOUTH, "PANEL DP-1", "MAIN FUSE F1", "USE TO OPEN", "");

        c.set(30, 0, 5, Blocks.SMOOTH_STONE);
        c.bindMachine(30, 1, 5, ModBlocks.PUMP_CONTROLLER.get(), Direction.SOUTH, BellwetherSite.NODE_PUMP);
        c.bindLamp(30, 2, 5, BellwetherSite.NODE_PUMP);
        c.wallSign(31, 1, 5, Direction.SOUTH, "PUMP P1", "5 UNITS", "GAUGE: PRESSURE", "");

        c.set(32, 0, 5, Blocks.SMOOTH_STONE);
        c.bindMachine(32, 1, 5, ModBlocks.RELAY_CONTROLLER.get(), Direction.SOUTH, BellwetherSite.NODE_RELAY);
        c.bindLamp(32, 2, 5, BellwetherSite.NODE_RELAY);
        c.wallSign(33, 1, 5, Direction.SOUTH, "REGIONAL RELAY", "R4 - 3 UNITS", "ANTENNA: TOWER", "");

        for (int x = 24; x <= 33; x++) {
            c.set(x, 3, 5, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.X));
        }

        // Spare fuse cabinet: two fuses, so a correct repair visibly uses exactly one.
        c.set(23, 0, 8, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST));
        if (c.level.getBlockEntity(c.rel(23, 0, 8)) instanceof Container box) {
            box.setItem(13, new ItemStack(ModItems.REPLACEMENT_FUSE.get(), 2));
        }
        c.wallSign(23, 1, 8, Direction.EAST, "SPARE FUSES", "TYPE F1", "", "");

        c.set(23, 0, 10, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        c.set(23, 0, 11, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        c.set(23, 1, 11, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 2));

        c.set(33, 0, 9, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        c.set(33, 0, 10, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        c.bindLamp(33, 1, 9, BellwetherSite.NODE_LOCAL_TX);
        c.set(33, 2, 9, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        c.wallSign(33, 1, 10, Direction.WEST, "LOCAL TX", "1 UNIT", "BELLWETHER", "MAINT. CHANNEL");

        c.set(30, 0, 12, Blocks.WHITE_CARPET);
        c.set(31, 0, 12, Blocks.GREEN_CARPET);
        c.set(32, 0, 12, Blocks.GREEN_CARPET);
        c.set(33, 0, 13, Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.WEST));

        c.set(25, 0, 11, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST));
        LecternBlock.tryPlaceBook(null, c.level, c.rel(25, 0, 11), c.level.getBlockState(c.rel(25, 0, 11)), maintenanceLog());

        c.set(26, 4, 8, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        c.set(31, 4, 9, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private static void pumpStand(SiteCanvas c) {
        c.fill(35, -1, 9, 39, -1, 13, Blocks.POLISHED_ANDESITE.defaultBlockState());
        c.set(35, 1, 11, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.EAST));
        c.set(36, 1, 11, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.EAST));
        c.set(37, 0, 11, Blocks.WAXED_OXIDIZED_CUT_COPPER);
        c.set(37, 1, 11, slab(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, SlabType.BOTTOM));
        c.set(38, 0, 10, Blocks.CAULDRON);
        c.set(38, 0, 11, Blocks.CAULDRON);
        c.bindings.put(c.rel(38, 0, 10), SiteProjector.NODE_PUMP_TANK);
        c.bindings.put(c.rel(38, 0, 11), SiteProjector.NODE_PUMP_TANK);
        c.set(36, 0, 13, Blocks.STONE_BRICK_WALL);
        c.bindLamp(36, 1, 13, BellwetherSite.NODE_PUMP);
        c.standingSign(38, 0, 13, 0, "PUMP SERVICE", "HEADER TANK", "TO BELLWETHER", "");
    }

    static ItemStack maintenanceLog() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : new String[]{
                "BELLWETHER RELAY STN. 4\nMaintenance log\n\nGenerator G1: 6 units\nPump P1: 5\nLocal TX: 1\nRegional relay R4: 3\n\nThe bus carries what G1 gives. No more.",
                "Day 212\nF1 running hot. Isolated the supply at G1 before it went. Pump's on the gravity bypass for now.\n\nWe have water. We can't tell the depot what we need.\n- N.S.",
                "F1 REPLACEMENT\n1. Confirm G1 isolated.\n2. Spare fuse from the cabinet by the bench.\n3. Fit F1 at DP-1.\n4. Close the isolator at G1.\n5. Set breakers at DP-1.\n   Check the total first.",
                "Day 209\nStorm knocked R4's antenna off its bearing. Relay will take power and reach nobody until it's realigned.\n\nThe tower ladder's sound. Don't trust the top rail.\n- N.S."}) {
            pages.add(Filterable.passThrough(Component.literal(page)));
        }
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Maintenance Log"), "N. Serrat", 0, pages, true));
        return book;
    }
}
