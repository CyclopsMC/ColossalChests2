package org.cyclops.colossalchests2.material;

import org.cyclops.cyclopscore.config.extendedconfig.ItemConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

/**
 * Config for the {@link ItemMaterialUpgradeTool}.
 * @author rubensworks
 */
public class ItemMaterialUpgradeToolConfig<M extends IModBase> extends ItemConfigCommon<M> {

    public ItemMaterialUpgradeToolConfig(M mod) {
        super(mod, "material_upgrade_tool", (eConfig, props) -> new ItemMaterialUpgradeTool(props.stacksTo(1)));
    }

}
