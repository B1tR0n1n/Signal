package io.github.b1tr0n1n.signal.scenario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Refuses to build over anything that looks like player work. Natural terrain (soil, stone, sand,
 * vegetation, trees, water, snow, ice) may be cleared; anything else, any block entity (chests, signs)
 * and any non-mob entity (item frames, armor stands, minecarts) blocks construction.
 */
public final class ScenarioGuard {
    private ScenarioGuard() {
    }

    public record Report(boolean ok, int occupied, @Nullable BlockPos first, @Nullable String reasonKey, String firstWhat) {
    }

    public static Report check(ServerLevel level, BoundingBox box) {
        return check(level, box, null);
    }

    /**
     * @param ground surveyed natural ground; when given, blocks more than 3 below the column's ground count as
     *               world-generated unless they carry a block entity (a player's underground base still has
     *               chests, furnaces or beds), so caves, lava lakes and geodes never block construction
     */
    public static Report check(ServerLevel level, BoundingBox box, @Nullable Terrain ground) {
        if (box.minY() < level.getMinBuildHeight() || box.maxY() >= level.getMaxBuildHeight()) {
            return new Report(false, 0, null, "command.signal.scenario.out_of_height", "");
        }
        if (!level.hasChunksAt(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            return new Report(false, 0, null, "command.signal.scenario.not_loaded", "");
        }
        int occupied = 0;
        BlockPos first = null;
        String what = "";
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int deep = ground != null && ground.contains(x, z) ? ground.at(x, z) - 3 : Integer.MIN_VALUE;
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    boolean hasEntity = level.getBlockEntity(p) != null;
                    if (hasEntity || y >= deep && !isNatural(s)) {
                        occupied++;
                        if (first == null) {
                            first = p.immutable();
                            what = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
                        }
                    }
                }
            }
        }
        AABB aabb = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        List<Entity> foreign = level.getEntities((Entity) null, aabb,
                e -> !(e instanceof Player) && !(e instanceof Mob) && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrb));
        if (!foreign.isEmpty()) {
            occupied += foreign.size();
            if (first == null) {
                first = foreign.get(0).blockPosition();
                what = foreign.get(0).getType().toShortString();
            }
        }
        return new Report(occupied == 0, occupied, first, occupied == 0 ? null : "command.signal.scenario.occupied", what);
    }

    public static boolean isNatural(BlockState s) {
        return s.isAir()
                || s.is(BlockTags.REPLACEABLE)
                || s.is(BlockTags.DIRT)
                || s.is(BlockTags.SAND)
                || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.LEAVES)
                || s.is(BlockTags.LOGS)
                || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.SAPLINGS)
                || s.is(BlockTags.SNOW)
                || s.is(BlockTags.ICE)
                || s.is(BlockTags.TERRACOTTA)
                || s.is(BlockTags.CAVE_VINES)
                || s.is(Tags.Blocks.ORES)
                || s.is(Tags.Blocks.GRAVELS)
                || s.is(Blocks.CLAY)
                || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.MOSS_CARPET)
                || s.is(Blocks.SUGAR_CANE)
                || s.is(Blocks.BAMBOO)
                || s.is(Blocks.CACTUS)
                || s.is(Blocks.PUMPKIN)
                || s.is(Blocks.MELON)
                || s.is(Blocks.LILY_PAD)
                || s.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || s.is(Blocks.RED_MUSHROOM_BLOCK)
                || s.is(Blocks.MUSHROOM_STEM)
                || s.is(Blocks.SWEET_BERRY_BUSH)
                || s.is(Blocks.MOSSY_COBBLESTONE) // forest_rock boulders
                || s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM)
                || s.is(Blocks.SANDSTONE)
                || s.is(Blocks.CALCITE) || s.is(Blocks.SMOOTH_BASALT) || s.is(Blocks.AMETHYST_BLOCK)
                || s.is(Blocks.BUDDING_AMETHYST) || s.is(Blocks.AMETHYST_CLUSTER)
                || s.is(Blocks.POINTED_DRIPSTONE) || s.is(Blocks.DRIPSTONE_BLOCK)
                || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.OBSIDIAN) // lake floors, lava meeting water
                || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT);
    }
}
