package org.cyclops.colossalchests2.blockentity;

import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import org.cyclops.cyclopscore.init.ModBaseFabric;

/**
 * Fabric config for the {@link BlockEntityUncolossalChest}, exposing its slots as item storage.
 * @author rubensworks
 */
public class BlockEntityUncolossalChestConfigFabric<M extends ModBaseFabric<?>> extends BlockEntityUncolossalChestConfig<M> {

    public BlockEntityUncolossalChestConfigFabric(M mod) {
        super(mod, BlockEntityUncolossalChest::new);
    }

    @Override
    public void onRegistryRegistered() {
        super.onRegistryRegistered();
        // All slots are open to all sides, so no direction: that avoids a sided wrapper per lookup.
        ItemStorage.SIDED.registerForBlockEntity((chest, side) -> ContainerStorage.of(chest, null), getInstance());
    }
}
