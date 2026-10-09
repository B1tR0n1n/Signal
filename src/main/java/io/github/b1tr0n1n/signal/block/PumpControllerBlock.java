package io.github.b1tr0n1n.signal.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Pump service state. {@code pressure} (0-3) is the gauge reading: it climbs while the pump has power
 * and falls when it loses power. Cosmetic presentation of the authoritative running state.
 */
public class PumpControllerBlock extends SiteMachineBlock {
    public static final IntegerProperty PRESSURE = IntegerProperty.create("pressure", 0, 3);

    public PumpControllerBlock(Properties props) {
        super(props);
        registerDefaultState(withDefaultFacing(stateDefinition.any()).setValue(PRESSURE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PRESSURE);
    }
}
