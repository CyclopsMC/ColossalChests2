package org.cyclops.colossalchests2.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.cyclops.colossalchests2.RegistryEntries;
import org.cyclops.colossalchests2.capability.ResourceHandlerChestStorage;

/**
 * NeoForge chest core, with one item resource handler per core so all lookups share transaction snapshots.
 * @author rubensworks
 */
public class BlockEntityChestCoreNeoForge extends BlockEntityChestCore {

    private final ResourceHandlerChestStorage resourceHandler;

    public BlockEntityChestCoreNeoForge(BlockPos pos, BlockState state) {
        super(RegistryEntries.BLOCK_ENTITY_CHEST_CORE.value(), pos, state);
        this.resourceHandler = new ResourceHandlerChestStorage(getStorage());
    }

    public ResourceHandlerChestStorage getResourceHandler() {
        return resourceHandler;
    }
}
