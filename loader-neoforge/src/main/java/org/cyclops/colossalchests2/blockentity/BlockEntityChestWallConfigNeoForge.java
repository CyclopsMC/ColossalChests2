package org.cyclops.colossalchests2.blockentity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.cyclops.cyclopscore.init.ModBaseNeoForge;

/**
 * NeoForge config for the {@link BlockEntityChestWall}, exposing item resource handlers on formed functional walls.
 * @author rubensworks
 */
public class BlockEntityChestWallConfigNeoForge<M extends ModBaseNeoForge<?>> extends BlockEntityChestWallConfig<M> {

    public BlockEntityChestWallConfigNeoForge(M mod) {
        super(mod, BlockEntityChestWall::new);
        mod.getModEventBus().addListener(this::registerCapabilities);
    }

    protected void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, getInstance(),
                (wall, side) -> wall.getExposedCore()
                        .map(core -> ((BlockEntityChestCoreNeoForge) core).getResourceHandler().withAccess(wall.getAccess()))
                        .orElse(null));
    }
}
