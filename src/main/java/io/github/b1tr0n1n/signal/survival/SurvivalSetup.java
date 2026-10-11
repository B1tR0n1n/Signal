package io.github.b1tr0n1n.signal.survival;

import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import io.github.b1tr0n1n.signal.registry.ModEntities;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Mod-bus registration for the survival layer: entity attributes and natural spawn rules. */
public final class SurvivalSetup {
    static final float NATURAL_CHANCE = 0.2F;
    static final double NEARBY_RADIUS = 48;
    static final int MAX_NEARBY = 3;

    @SubscribeEvent
    public void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DRAWN.get(), DrawnEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void spawns(RegisterSpawnPlacementsEvent event) {
        // Dark, solid ground, like any monster; which biomes they appear in is the biome's spawn list. Natural
        // spawns are rare and never crowd: a fifth of attempts, and not with MAX_NEARBY already close.
        event.register(ModEntities.DRAWN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                        && (reason != MobSpawnType.NATURAL || random.nextFloat() < NATURAL_CHANCE
                        && level.getEntitiesOfClass(DrawnEntity.class, new AABB(pos).inflate(NEARBY_RADIUS)).size() < MAX_NEARBY),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
