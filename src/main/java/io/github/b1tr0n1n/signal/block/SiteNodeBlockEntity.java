package io.github.b1tr0n1n.signal.block;

import io.github.b1tr0n1n.signal.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Local identity only: which campaign node this physical block stands for. Service state is never
 * stored here; it lives in the campaign SavedData and is projected onto block states.
 */
public class SiteNodeBlockEntity extends BlockEntity {
    private static final String TAG_NODE = "SignalNode";
    private String nodeId = "";

    public SiteNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SITE_NODE.get(), pos, state);
    }

    public String nodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TAG_NODE, nodeId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nodeId = tag.getString(TAG_NODE);
    }
}
