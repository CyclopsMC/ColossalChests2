package org.cyclops.colossalchests2.storage;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import org.cyclops.colossalchests2.Reference;

/**
 * Keeps chest cores that carry items out of chests, so cores can not be nested in each other without limit and
 * overflow the size of saved data. Also looks inside vanilla containers, such as shulker boxes and bundles.
 * @author rubensworks
 */
public final class NestedChests {

    /**
     * The item component of a core item that carries the chest's contents.
     */
    public static final Identifier CONTENTS_COMPONENT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "chest_contents");
    // Deeper nesting is refused outright, so a crafted stack can not make this check itself expensive.
    private static final int MAX_DEPTH = 8;

    private NestedChests() {
    }

    /**
     * @param stack An item stack.
     * @return If a chest may store the stack: it is not a core with contents, and holds none.
     */
    public static boolean canStore(ItemStack stack) {
        return !holdsChestContents(stack, 0);
    }

    private static boolean holdsChestContents(ItemStack stack, int depth) {
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return false;
        }
        if (depth >= MAX_DEPTH) {
            return true;
        }
        // Compared by id, so this works without the mod's registries, such as in unit tests.
        for (DataComponentType<?> type : patch.split().added().keySet()) {
            if (CONTENTS_COMPONENT.equals(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type))) {
                return true;
            }
        }
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            for (ItemStack inner : container.nonEmptyItemCopyStream().toList()) {
                if (holdsChestContents(inner, depth + 1)) {
                    return true;
                }
            }
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (ItemStack inner : bundle.itemCopies().toList()) {
                if (holdsChestContents(inner, depth + 1)) {
                    return true;
                }
            }
        }
        return false;
    }
}
