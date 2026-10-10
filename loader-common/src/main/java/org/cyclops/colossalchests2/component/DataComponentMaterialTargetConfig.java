package org.cyclops.colossalchests2.component;

import net.minecraft.resources.Identifier;
import org.cyclops.cyclopscore.config.extendedconfig.DataComponentConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

/**
 * Config for the material a Material Upgrade Tool changes chests to.
 * @author rubensworks
 */
public class DataComponentMaterialTargetConfig<M extends IModBase> extends DataComponentConfigCommon<Identifier, M> {

    public DataComponentMaterialTargetConfig(M mod) {
        super(mod, "material_target", builder -> builder
                .persistent(Identifier.CODEC)
                .networkSynchronized(Identifier.STREAM_CODEC));
    }

}
