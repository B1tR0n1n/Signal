package io.github.b1tr0n1n.signal.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Allocation and preview interface. {@code fault} shows the scorched F1 carrier until the fuse is replaced. */
public class DistributionPanelBlock extends SiteMachineBlock {
    public static final BooleanProperty FAULT = BooleanProperty.create("fault");

    public DistributionPanelBlock(Properties props) {
        super(props);
        registerDefaultState(withDefaultFacing(stateDefinition.any()).setValue(FAULT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FAULT);
    }
}
