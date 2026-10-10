package org.cyclops.colossalchests2.blockentity;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.cyclops.colossalchests2.client.render.RenderChestCore;
import org.cyclops.colossalchests2.client.render.RenderChestCoreNeoForge;
import org.cyclops.colossalchests2.network.ChestNetwork;
import org.cyclops.cyclopscore.init.ModBaseNeoForge;

/**
 * NeoForge config for the {@link BlockEntityChestCore}, exposing item resource handlers on formed cores.
 * @author rubensworks
 */
public class BlockEntityChestCoreConfigNeoForge<M extends ModBaseNeoForge<?>> extends BlockEntityChestCoreConfig<M> {

    public BlockEntityChestCoreConfigNeoForge(M mod) {
        super(mod, BlockEntityChestCoreNeoForge::new);
        mod.getModEventBus().addListener(this::registerCapabilities);
        BlockEntityChestCore.capabilityInvalidator = Level::invalidateCapabilities;
        ChestNetwork.canReceive = (player, packet) -> player.connection.hasChannel(packet);
    }

    @Override
    protected BlockEntityRendererProvider<BlockEntityChestCore, RenderChestCore.State> getRendererProvider() {
        return RenderChestCoreNeoForge::new;
    }

    protected void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, getInstance(),
                (core, side) -> core.isFormed() ? ((BlockEntityChestCoreNeoForge) core).getResourceHandler() : null);
    }
}
