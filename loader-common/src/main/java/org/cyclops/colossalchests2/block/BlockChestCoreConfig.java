package org.cyclops.colossalchests2.block;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import org.cyclops.colossalchests2.api.ChestMaterial;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

import java.util.function.Supplier;

/**
 * Config for a {@link BlockChestCore} of one material.
 * @author rubensworks
 */
public class BlockChestCoreConfig<M extends IModBase> extends BlockConfigCommon<M> {

    /**
     * @param mod The mod registering the block, named chest_core_[material name].
     * @param material The material.
     * @param properties Creates the block properties, such as hardness and sound.
     */
    public BlockChestCoreConfig(M mod, ChestMaterial material, Supplier<Block.Properties> properties) {
        super(
                mod,
                "chest_core_" + material.getName(),
                (eConfig, props) -> new BlockChestCore(properties.get().setId(ResourceKey.create(Registries.BLOCK, eConfig.getResourceKey().identifier())), material),
                ItemBlockTooltip.constructor()
        );
    }

}
