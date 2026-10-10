package io.github.b1tr0n1n.signal.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Authored supply source. {@code running} is projected from the campaign: true when the bus is live. */
public class GeneratorBlock extends SiteMachineBlock {
    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);

    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public GeneratorBlock(Properties props) {
        super(props);
        registerDefaultState(withDefaultFacing(stateDefinition.any()).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RUNNING);
    }


    @Override
    protected MapCodec<? extends GeneratorBlock> codec() {
        return CODEC;
    }
}
