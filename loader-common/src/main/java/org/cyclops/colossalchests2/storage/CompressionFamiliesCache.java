package org.cyclops.colossalchests2.storage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The compression families found in the server's recipes, rebuilt when the recipes change.
 * Clients do not receive recipes, so they use the conversions the server sends them.
 * @author rubensworks
 */
public final class CompressionFamiliesCache {

    private static final Map<RecipeManager, Entry> CACHE = new WeakHashMap<>();
    private static CompressionFamilies clientFamilies = CompressionFamilies.EMPTY;

    private CompressionFamiliesCache() {
    }

    /**
     * @param level A level.
     * @return The compression families of its recipes.
     */
    public static synchronized CompressionFamilies get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return clientFamilies;
        }
        RecipeManager manager = serverLevel.getServer().getRecipeManager();
        // A reload replaces the manager's recipe map rather than the manager.
        Collection<?> recipes = manager.getRecipes();
        Entry entry = CACHE.get(manager);
        if (entry == null || entry.recipes() != recipes) {
            List<CompressionDiscovery.Conversion> conversions = CompressionDiscovery.findConversions(manager);
            entry = new Entry(recipes, conversions, CompressionDiscovery.buildFamilies(conversions));
            CACHE.put(manager, entry);
        }
        return entry.families();
    }

    /**
     * @param level A server level.
     * @return The conversions behind its compression families, to send to clients.
     */
    public static synchronized List<CompressionDiscovery.Conversion> getConversions(ServerLevel level) {
        get(level);
        return CACHE.get(level.getServer().getRecipeManager()).conversions();
    }

    /**
     * Set the conversions received from the server.
     */
    public static synchronized void setClientConversions(List<CompressionDiscovery.Conversion> conversions) {
        clientFamilies = CompressionDiscovery.buildFamilies(conversions);
    }

    private record Entry(Collection<?> recipes, List<CompressionDiscovery.Conversion> conversions, CompressionFamilies families) {
    }

}
