package org.cyclops.colossalchests2.upgrade;

import org.cyclops.colossalchests2.api.upgrade.ChestUpgrade;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

/**
 * Config for an upgrade item.
 * @author rubensworks
 */
public class ItemChestUpgradeConfig<M extends IModBase> extends ItemConfigCommon<M> {

    public ItemChestUpgradeConfig(M mod, ChestUpgrade upgrade) {
        super(mod, "upgrade_" + upgrade.getId().getPath(), (eConfig, props) -> new ItemChestUpgrade(props.stacksTo(16), upgrade));
    }

}
