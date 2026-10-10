package org.cyclops.colossalchests2.blockentity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.cyclops.cyclopscore.init.ModBaseNeoForge;

/**
 * NeoForge config for the {@link BlockEntityUncolossalChest}, exposing its slots as an item resource handler.
 * @author rubensworks
 */
public class BlockEntityUncolossalChestConfigNeoForge<M extends ModBaseNeoForge<?>> extends BlockEntityUncolossalChestConfig<M> {

    public BlockEntityUncolossalChestConfigNeoForge(M mod) {
        super(mod, BlockEntityUncolossalChest::new);
        mod.getModEventBus().addListener(this::registerCapabilities);
    }

    protected void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, getInstance(), (chest, side) -> VanillaContainerWrapper.of(chest));
    }
}
