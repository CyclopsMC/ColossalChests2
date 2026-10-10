package org.cyclops.colossalchests2.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.cyclops.colossalchests2.block.BlockChestCore;
import org.cyclops.colossalchests2.block.BlockChestFunctionalWall;
import org.cyclops.colossalchests2.block.WallType;
import org.cyclops.colossalchests2.blockentity.BlockEntityChestCore;
import org.cyclops.colossalchests2.blockentity.BlockEntityChestWall;
import org.cyclops.colossalchests2.capability.ItemHandlerLogic;
import org.cyclops.colossalchests2.multiblock.ChestShape;
import org.cyclops.colossalchests2.multiblock.ChestStructure;
import org.cyclops.colossalchests2.storage.ChestStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lets copper golems use colossal chests. Golems take items out through Golem walls, like from copper chests, and
 * sort items into any chest without one, like into vanilla chests. As a chest with a Golem wall is never a
 * destination, golems do not put items back where they took them.
 * @author rubensworks
 */
public final class GolemTransport {

    /**
     * The most items a golem takes at once, as from a copper chest.
     */
    public static final int PICKUP_AMOUNT = 16;

    private GolemTransport() {
    }

    /**
     * @param state A block state.
     * @return If golems take items out of a block in this state.
     */
    public static boolean isSource(BlockState state) {
        return state.getBlock() instanceof BlockChestFunctionalWall wall && wall.getType() == WallType.GOLEM
                && state.getValue(BlockChestFunctionalWall.FORMED);
    }

    /**
     * @param state A block state.
     * @return If golems may put items into a block in this state.
     */
    public static boolean isDestination(BlockState state) {
        return state.getBlock() instanceof BlockChestCore && state.getValue(BlockChestCore.FORMED);
    }

    /**
     * @param blockEntity A block entity.
     * @param pickingUp If the golem has no item, and so looks for a source.
     * @return If the golem may target this block entity.
     */
    public static boolean isTarget(BlockEntity blockEntity, boolean pickingUp) {
        return getContainer(blockEntity) instanceof ChestContainer container && container.isSource() == pickingUp
                && (pickingUp || !hasGolemWall(container.getCore()))
                && getApproachPos(container, blockEntity.getBlockPos()) != null;
    }

    /**
     * Vanilla paths to the first free block above a target, which is the top of a colossal chest, so golems walk to
     * the side of the target instead.
     * @param container The container of a target.
     * @param pos The target position.
     * @return The position in front of a side of the target on the outside of its chest, or null if none.
     */
    @Nullable
    public static BlockPos getApproachPos(ChestContainer container, BlockPos pos) {
        ChestStructure structure = container.getCore().getStructure();
        if (structure == null) {
            return null;
        }
        for (Direction face : ChestShape.getOuterFaces(structure, pos)) {
            if (face.getAxis().isHorizontal()) {
                return pos.relative(face);
            }
        }
        return null;
    }

    /**
     * Golems standing in front of a chest reach a target above them, up to the floor of the chest, as vanilla reach
     * only allows for targets at the height of the golem.
     * @param container The container of a target.
     * @param pos The target position.
     * @return The area a golem must touch to reach the target.
     */
    public static AABB getReachArea(ChestContainer container, BlockPos pos) {
        ChestStructure structure = container.getCore().getStructure();
        int minY = structure == null ? pos.getY() : structure.min().getY();
        return new AABB(pos.getX(), minY, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    /**
     * @param blockEntity A block entity.
     * @return The container golems use for it, or null if it is not part of a formed colossal chest.
     */
    @Nullable
    public static Container getContainer(BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityChestWall wall && wall.getWallType() == WallType.GOLEM) {
            return wall.getCore().map(core -> new ChestContainer(core, true)).orElse(null);
        }
        if (blockEntity instanceof BlockEntityChestCore core && core.isFormed()) {
            return new ChestContainer(core, false);
        }
        return null;
    }

    /**
     * @param core A formed chest.
     * @return If golems take items out of the chest, so it must not receive them.
     */
    public static boolean hasGolemWall(BlockEntityChestCore core) {
        Level level = core.getLevel();
        if (level == null) {
            return false;
        }
        for (BlockPos pos : core.getDecoratedPositions()) {
            if (isSource(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param container A golem source.
     * @return If there is anything to take.
     */
    public static boolean canPickUp(ChestContainer container) {
        ItemHandlerLogic logic = container.getLogic();
        for (int slot = 0; slot < logic.getSlots(); slot++) {
            if (!logic.extractItem(slot, 1, true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Take up to {@link #PICKUP_AMOUNT} of the first item, as golems do from copper chests.
     * @param container A golem source.
     * @return The taken items.
     */
    public static ItemStack pickUp(ChestContainer container) {
        ItemHandlerLogic logic = container.getLogic();
        for (int slot = 0; slot < logic.getSlots(); slot++) {
            ItemStack extracted = logic.extractItem(slot, PICKUP_AMOUNT, false);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Like for vanilla chests, golems only sort items into an empty chest or one that already holds that item.
     * @param body The golem.
     * @param container A golem destination.
     * @return If the golem may put down its held item.
     */
    public static boolean canPutDown(PathfinderMob body, ChestContainer container) {
        ItemStack held = body.getMainHandItem();
        ChestStorage storage = container.getCore().getStorage();
        if (hasGolemWall(container.getCore()) || storage.insertAutomated(held, held.getCount(), true) <= 0) {
            return false;
        }
        return container.isEmpty() || holds(storage, held);
    }

    /**
     * @param body The golem.
     * @param container A golem destination.
     * @return What is left of the held item.
     */
    public static ItemStack putDown(PathfinderMob body, ChestContainer container) {
        ItemStack held = body.getMainHandItem();
        long inserted = container.getCore().getStorage().insertAutomated(held, held.getCount(), false);
        return held.copyWithCount(held.getCount() - (int) inserted);
    }

    private static boolean holds(ChestStorage storage, ItemStack type) {
        for (int slot = 0; slot < storage.getSlotCount(); slot++) {
            if (ItemStack.isSameItem(storage.getSlotType(slot), type) || ItemStack.isSameItem(storage.getExtractionType(slot), type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The container golems see for a colossal chest. Golem behavior only uses it through {@link GolemTransport}, so
     * it exposes no slots of its own. Opening it opens the lid of the chest.
     */
    public static final class ChestContainer implements Container {

        private final BlockEntityChestCore core;
        private final boolean source;

        public ChestContainer(BlockEntityChestCore core, boolean source) {
            this.core = core;
            this.source = source;
        }

        public BlockEntityChestCore getCore() {
            return core;
        }

        /**
         * @return If golems take items out, instead of putting them in.
         */
        public boolean isSource() {
            return source;
        }

        private ItemHandlerLogic getLogic() {
            return core.getItemHandlerLogic();
        }

        @Override
        public int getContainerSize() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            ChestStorage storage = core.getStorage();
            for (int slot = 0; slot < storage.getSlotCount(); slot++) {
                if (storage.getSlotAmount(slot) > 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
        }

        @Override
        public void setChanged() {
            core.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
        }

        @Override
        public void startOpen(ContainerUser containerUser) {
            core.addOpener(containerUser);
        }

        @Override
        public void stopOpen(ContainerUser containerUser) {
            core.removeOpener(containerUser);
        }

        @Override
        public List<ContainerUser> getEntitiesWithContainerOpen() {
            return core.getOpeners();
        }
    }
}
