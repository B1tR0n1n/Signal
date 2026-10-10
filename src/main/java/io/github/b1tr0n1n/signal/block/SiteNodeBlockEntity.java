package io.github.b1tr0n1n.signal.block;

import io.github.b1tr0n1n.signal.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_NODE, nodeId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        nodeId = tag.getString(TAG_NODE);
    }
}
