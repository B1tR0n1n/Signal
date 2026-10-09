package io.github.b1tr0n1n.signal.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class RelayControllerBlock extends SiteMachineBlock {
    public static final EnumProperty<RelayState> RELAY = EnumProperty.create("relay", RelayState.class);

    public RelayControllerBlock(Properties props) {
        super(props);
        registerDefaultState(withDefaultFacing(stateDefinition.any()).setValue(RELAY, RelayState.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RELAY);
    }
}
