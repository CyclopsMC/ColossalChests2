package org.cyclops.colossalchests2.modcompat;

import org.cyclops.colossalchests2.ColossalChests;
import org.cyclops.colossalchests2.Reference;
import org.cyclops.cyclopscore.modcompat.ICompatInitializer;
import org.cyclops.cyclopscore.modcompat.IModCompat;

/**
 * Exposes the inventory state of chests through Common Capabilities, so consumers can skip rescanning unchanged chests.
 * Must not refer to Common Capabilities types, as it is loaded without that mod.
 * @author rubensworks
 */
public class CommonCapabilitiesModCompat implements IModCompat {

    @Override
    public String getId() {
        return Reference.MOD_COMMONCAPABILITIES;
    }

    @Override
    public boolean isEnabledDefault() {
        return true;
    }

    @Override
    public String getComment() {
        return "If the inventory state capability should be exposed on chests.";
    }

    @Override
    public ICompatInitializer createInitializer() {
        return () -> ColossalChests._instance.getModEventBus().addListener(InventoryStateCapabilities::register);
    }
}
