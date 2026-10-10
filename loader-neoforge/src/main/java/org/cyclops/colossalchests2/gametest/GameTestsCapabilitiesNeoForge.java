package org.cyclops.colossalchests2.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.cyclops.colossalchests2.Reference;
import org.cyclops.colossalchests2.RegistryEntries;
import org.cyclops.colossalchests2.api.ChestMaterial;
import org.cyclops.colossalchests2.block.WallType;
import org.cyclops.colossalchests2.capability.ResourceHandlerChestStorage;
import org.cyclops.colossalchests2.capability.WallAccess;
import org.cyclops.colossalchests2.modcompat.InventoryStateChestStorage;
import org.cyclops.colossalchests2.storage.CapacityProfile;
import org.cyclops.colossalchests2.storage.ChestStorage;
import org.cyclops.commoncapabilities.api.capability.inventorystate.IInventoryState;
import org.cyclops.cyclopscore.gametest.GameTest;

/**
 * Item resource handler tests through NeoForge's own transfer helpers.
 * @author rubensworks
 */
public class GameTestsCapabilitiesNeoForge {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
    private static final ItemResource STONE = ItemResource.of(Items.STONE);

    private static ChestStorage createStorage() {
        return new ChestStorage(3, CapacityProfile.ofDepth(4));
    }

    private static int insert(ResourceHandler<ItemResource> handler, int index, ItemResource resource, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(index, resource, amount, tx);
            tx.commit();
            return inserted;
        }
    }

    private static int extract(ResourceHandler<ItemResource> handler, int index, ItemResource resource, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(index, resource, amount, tx);
            tx.commit();
            return extracted;
        }
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testInsertStackedMergesIntoMatchingSlot(GameTestHelper helper) {
        ChestStorage storage = createStorage();
        ResourceHandler<ItemResource> handler = new ResourceHandlerChestStorage(storage);
        storage.insert(2, STONE.toStack(), 10, false);

        int inserted = ResourceHandlerUtil.insertStacking(handler, STONE, 64, null);

        helper.assertValueEqual(inserted, 64, "inserted");
        helper.assertValueEqual(storage.getSlot(2).getCount(), 74L, "merged count");
        helper.assertTrue(storage.getSlot(0).isEmpty(), "Expected slot 0 to stay empty");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testAbortedTransactionLeavesStorageUntouched(GameTestHelper helper) {
        ChestStorage storage = createStorage();
        ResourceHandlerChestStorage handler = new ResourceHandlerChestStorage(storage);
        storage.insert(1, STONE.toStack(), 10, false);
        ResourceHandler<ItemResource> wallView = handler.withAccess(WallAccess.OPEN);

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(0, STONE, 64, tx), 64, "inserted in the transaction");
            helper.assertValueEqual(wallView.extract(1, STONE, 5, tx), 5, "extracted through a wall view");
            helper.assertValueEqual(storage.getSlot(0).getCount(), 64L, "count inside the transaction");
            // Not committed.
        }

        helper.assertTrue(storage.getSlot(0).isEmpty(), "Expected the insert to be rolled back");
        helper.assertValueEqual(storage.getSlot(1).getCount(), 10L, "count after rolling back the extract");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testMoveAllToVanillaContainer(GameTestHelper helper) {
        ChestStorage storage = createStorage();
        ResourceHandler<ItemResource> handler = new ResourceHandlerChestStorage(storage);
        storage.insert(0, STONE.toStack(), 200, false);
        ResourceHandler<ItemResource> target = new ItemStacksResourceHandler(27);

        int moved = ResourceHandlerUtil.move(handler, target, resource -> true, Integer.MAX_VALUE, null);

        helper.assertValueEqual(moved, 200, "moved count");
        helper.assertTrue(storage.getSlot(0).isEmpty(), "Expected the chest slot to be empty");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testCountAboveIntReportedAsLong(GameTestHelper helper) {
        ChestStorage storage = new ChestStorage(1, CapacityProfile.ofDepth(1L << 40).withMaxItemsPerSlot(Long.MAX_VALUE));
        ResourceHandler<ItemResource> handler = new ResourceHandlerChestStorage(storage);
        storage.insert(0, STONE.toStack(), Integer.MAX_VALUE + 10L, false);

        helper.assertValueEqual(handler.getAmountAsLong(0), Integer.MAX_VALUE + 10L, "long count");
        helper.assertValueEqual(handler.getAmountAsInt(0), Integer.MAX_VALUE, "clamped count");
        helper.assertTrue(handler.getCapacityAsLong(0, STONE) > Integer.MAX_VALUE, "Expected a long capacity");
        helper.assertValueEqual(extract(handler, 0, STONE, 64), 64, "extracted count");
        helper.assertValueEqual(storage.getSlot(0).getCount(), Integer.MAX_VALUE + 10L - 64, "remaining count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testInventoryStateChangesOnlyOnChange(GameTestHelper helper) {
        ChestStorage storage = createStorage();
        ResourceHandler<ItemResource> handler = new ResourceHandlerChestStorage(storage);
        IInventoryState state = new InventoryStateChestStorage(storage);

        int initial = state.getState();
        try (Transaction tx = Transaction.openRoot()) {
            handler.insert(0, STONE, 5, tx);
        }
        helper.assertValueEqual(storage.getSlot(0).getCount(), 0L, "count after an aborted insert");
        insert(handler, 0, STONE, 5);
        int afterInsert = state.getState();
        helper.assertTrue(afterInsert != initial, "Expected the state to change after an insert");
        extract(handler, 0, STONE, 1);
        helper.assertTrue(state.getState() != afterInsert, "Expected the state to change after an extract");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testCapabilitiesOnFormedChest(GameTestHelper helper) {
        BlockPos min = new BlockPos(1, 1, 1);
        BlockPos corePos = GameTestsCommon.buildChest(helper, min, 3, ChestMaterial.WOOD);
        BlockPos wallPos = GameTestsCommon.placeWall(helper, min.offset(1, 2, 1), WallType.INTERFACE);
        BlockPos plainWallPos = min.offset(2, 1, 1);
        BlockPos brokenWall = min.offset(0, 1, 1);
        helper.startSequence()
                .thenWaitUntil(() -> GameTestsCommon.assertFormed(helper, corePos, min, 3))
                .thenExecute(() -> {
                    ResourceHandler<ItemResource> coreHandler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(corePos), Direction.NORTH);
                    ResourceHandler<ItemResource> wallHandler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(wallPos), Direction.UP);
                    IInventoryState state = helper.getLevel().getCapability(org.cyclops.commoncapabilities.api.capability.Capabilities.InventoryState.BLOCK, helper.absolutePos(wallPos), Direction.UP);
                    helper.assertTrue(coreHandler != null && wallHandler != null && state != null, "Expected capabilities on a formed chest");
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(plainWallPos), Direction.EAST) == null,
                            "Expected no item handler on a plain wall");
                    int initialState = state.getState();
                    helper.assertValueEqual(insert(wallHandler, 0, STONE, 10), 10, "inserted through the wall");
                    helper.assertValueEqual(coreHandler.getAmountAsLong(0), 10L, "count through the core");
                    helper.assertTrue(state.getState() != initialState, "Expected the inventory state to change");
                    helper.setBlock(brokenWall, Blocks.AIR);
                })
                .thenWaitUntil(() -> GameTestsCommon.assertDormant(helper, corePos))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(corePos), Direction.NORTH) == null, "Expected no item handler on a dormant core");
                    helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(wallPos), Direction.UP) == null, "Expected no item handler on a dormant wall");
                    helper.assertTrue(helper.getLevel().getCapability(org.cyclops.commoncapabilities.api.capability.Capabilities.InventoryState.BLOCK, helper.absolutePos(corePos), Direction.NORTH) == null, "Expected no inventory state on a dormant core");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testUncolossalChestItemHandler(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, RegistryEntries.BLOCK_UNCOLOSSAL_CHEST.value());
        ResourceHandler<ItemResource> handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(handler != null, "Expected an item handler");
        helper.assertValueEqual(handler.size(), 5, "slots");
        helper.assertValueEqual(insert(handler, 4, STONE, 10), 10, "inserted");
        helper.assertValueEqual(((Container) helper.getBlockEntity(pos, BlockEntity.class)).getItem(4).getCount(), 10, "stored count");
        helper.succeed();
    }

}
