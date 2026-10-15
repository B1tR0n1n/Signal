package io.github.b1tr0n1n.signal.survival;

import io.github.b1tr0n1n.signal.domain.DrawnBudget;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import io.github.b1tr0n1n.signal.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.function.Predicate;

/** Mod-bus registration for the survival layer: entity attributes and natural spawn rules. */
public final class SurvivalSetup {
    static final float NATURAL_CHANCE = 0.2F;

    @SubscribeEvent
    public void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DRAWN.get(), DrawnEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void spawns(RegisterSpawnPlacementsEvent event) {
        // Dark, solid ground, like any monster; which biomes they appear in is the biome's spawn list. Natural
        // spawns are rare and never crowd: a fifth of attempts, and only within the per-player DrawnBudget.
        event.register(ModEntities.DRAWN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                        && (reason != MobSpawnType.NATURAL || random.nextFloat() < NATURAL_CHANCE && withinBudget(level.getLevel(), pos)),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /**
     * Under open sky only: they hunt the island at night, not the caves. Cave spawns out of ticking range never
     * moved, never left at dawn, and used up the whole budget so the surface stayed empty (seed 1, 2026-10-14).
     * The budget counts the Drawn in the 128-block range that natural spawning and despawning work in.
     */
    static boolean withinBudget(ServerLevel level, BlockPos pos) {
        if (!level.canSeeSky(pos)) {
            return false;
        }
        // Counted around the player the spawn is for, not around the spawn point: centred on the spawn point,
        // spawns on either side of the player each saw room and the player had 6 within range.
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, DrawnBudget.RANGE, false);
        if (player == null) {
            return false;
        }
        AABB range = player.getBoundingBox().inflate(DrawnBudget.RANGE);
        // Every living Drawn in range counts, frozen ones too: one that wanders past the ticking edge stops but is
        // still there when the player walks toward it. Counting only ticking ones let the outer ring fill to 14.
        // (Living only: one killed out of ticking range never finishes dying.)
        Predicate<DrawnEntity> active = DrawnEntity::isAlive;
        int around = level.getEntitiesOfClass(DrawnEntity.class, range, active).size();
        int near = level.getEntitiesOfClass(DrawnEntity.class, new AABB(pos).inflate(DrawnBudget.NEAR_RADIUS), active).size();
        int company = level.getEntitiesOfClass(Player.class, range, p -> !p.isSpectator()).size();
        return DrawnBudget.allowsNaturalSpawn(around, near, company);
    }
}
