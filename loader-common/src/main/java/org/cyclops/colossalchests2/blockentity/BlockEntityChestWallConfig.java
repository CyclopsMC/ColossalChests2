package org.cyclops.colossalchests2.blockentity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.cyclops.colossalchests2.Reference;
import org.cyclops.colossalchests2.api.client.ChestOverlays;
import org.cyclops.colossalchests2.api.client.FunctionalWallOverlay;
import org.cyclops.colossalchests2.block.BlockChestFunctionalWall;
import org.cyclops.colossalchests2.block.WallType;
import org.cyclops.colossalchests2.client.render.DisplayWallOverlay;
import org.cyclops.cyclopscore.config.extendedconfig.BlockEntityConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

import java.util.Set;

/**
 * Config for the {@link BlockEntityChestWall}.
 * @author rubensworks
 */
public class BlockEntityChestWallConfig<M extends IModBase> extends BlockEntityConfigCommon<BlockEntityChestWall, M> {

    public BlockEntityChestWallConfig(M mod, BlockEntityType.BlockEntitySupplier<? extends BlockEntityChestWall> supplier) {
        super(
                mod,
                "chest_wall",
                eConfig -> new BlockEntityType<>(supplier, Set.copyOf(BlockChestFunctionalWall.getFunctionalInstances()))
        );
    }

    @Override
    public void onRegistryRegistered() {
        super.onRegistryRegistered();
        if (getMod().getModHelpers().getMinecraftHelpers().isClientSide()) {
            for (BlockChestFunctionalWall wall : BlockChestFunctionalWall.getFunctionalInstances()) {
                ChestOverlays.register(wall, wall.getType() == WallType.DISPLAY ? new DisplayWallOverlay() : new FunctionalWallOverlay(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/" + wall.getType().getRegistryName() + "_icon")));
            }
        }
    }

}
