package org.cyclops.colossalchests2.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.cyclops.colossalchests2.storage.ChestStorage;
import org.cyclops.colossalchests2.storage.DeepSlot;

/**
 * NeoForge item resource handler on a chest storage, with long amounts and transaction support.
 * A transaction snapshots all slots, so an aborted transaction leaves the storage untouched.
 * Views for walls share the snapshots of the core's handler, so one transaction through several walls rolls back correctly.
 * Single operations are limited to {@link Integer#MAX_VALUE}, but {@link #getAmountAsLong(int)} and
 * {@link #getCapacityAsLong(int, ItemResource)} report the full values.
 * @author rubensworks
 */
public class ResourceHandlerChestStorage extends SnapshotJournal<DeepSlot[]> implements ResourceHandler<ItemResource> {

    private final ChestStorage storage;
    private final ItemHandlerLogic logic;
    private final SnapshotJournal<DeepSlot[]> snapshots;

    public ResourceHandlerChestStorage(ChestStorage storage) {
        this.storage = storage;
        this.logic = new ItemHandlerLogic(storage);
        this.snapshots = this;
    }

    private ResourceHandlerChestStorage(ResourceHandlerChestStorage parent, WallAccess access) {
        this.storage = parent.storage;
        this.logic = new ItemHandlerLogic(storage, access);
        this.snapshots = parent;
    }

    /**
     * @param access What automation may do through the view.
     * @return A view on the same storage, limited by the access rules.
     */
    public ResourceHandlerChestStorage withAccess(WallAccess access) {
        return new ResourceHandlerChestStorage(this, access);
    }

    public ChestStorage getStorage() {
        return storage;
    }

    private WallAccess access() {
        return logic.getAccess();
    }

    @Override
    public int size() {
        return storage.getSlotCount();
    }

    @Override
    public ItemResource getResource(int index) {
        return getAmountAsLong(index) == 0 ? ItemResource.EMPTY : ItemResource.of(logic.getExtractionType(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        ItemStack type = logic.getExtractionType(index);
        return type.isEmpty() ? 0 : storage.getAvailable(index, type);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        ItemStack type = resource.isEmpty() ? logic.getExtractionType(index) : resource.toStack();
        return type.isEmpty() ? storage.getCapacity(index) : storage.getCapacity(type);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && logic.isItemValid(index, resource.toStack());
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack type = resource.toStack();
        if (!access().canInsert(storage, type)) {
            return 0;
        }
        long inserted = storage.insertAutomated(index, type, amount, true, access().voidFull());
        if (inserted > 0) {
            snapshots.updateSnapshots(transaction);
            storage.insertAutomated(index, type, amount, false, access().voidFull());
        }
        return (int) inserted;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack type = resource.toStack();
        if (!access().canInsert(storage, type)) {
            return 0;
        }
        long inserted = storage.insertAutomated(type, amount, true, access().voidFull());
        if (inserted > 0) {
            snapshots.updateSnapshots(transaction);
            storage.insertAutomated(type, amount, false, access().voidFull());
        }
        return (int) inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        // Any form of a compressed slot's family can be extracted.
        ItemStack type = resource.toStack();
        if (!access().canExtract(storage, type)) {
            return 0;
        }
        long extracted = storage.extract(index, type, amount, true);
        if (extracted > 0) {
            snapshots.updateSnapshots(transaction);
            storage.extract(index, type, amount, false);
        }
        return (int) extracted;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack type = resource.toStack();
        if (!access().canExtract(storage, type)) {
            return 0;
        }
        long extracted = storage.extract(type, amount, true);
        if (extracted > 0) {
            snapshots.updateSnapshots(transaction);
            storage.extract(type, amount, false);
        }
        return (int) extracted;
    }

    @Override
    protected DeepSlot[] createSnapshot() {
        return storage.snapshotSlots();
    }

    @Override
    protected void revertToSnapshot(DeepSlot[] snapshot) {
        storage.restoreSlots(snapshot);
    }
}
