package org.cyclops.colossalchests2.modcompat;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.cyclops.colossalchests2.RegistryEntries;
import org.cyclops.commoncapabilities.api.capability.Capabilities;
import org.cyclops.commoncapabilities.api.capability.inventorystate.IInventoryState;

/**
 * Registers the inventory state capability of chests.
 * Kept apart from {@link CommonCapabilitiesModCompat}, as loading this class requires Common Capabilities.
 * @author rubensworks
 */
public final class InventoryStateCapabilities {

    private InventoryStateCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.InventoryState.BLOCK, RegistryEntries.BLOCK_ENTITY_CHEST_CORE.value(),
                (core, side) -> core.isFormed() ? new InventoryStateChestStorage(core.getStorage()) : null);
        event.registerBlockEntity(Capabilities.InventoryState.BLOCK, RegistryEntries.BLOCK_ENTITY_CHEST_WALL.value(),
                (wall, side) -> wall.getExposedCore().map(core -> (IInventoryState) new InventoryStateChestStorage(core.getStorage())).orElse(null));
    }
}
