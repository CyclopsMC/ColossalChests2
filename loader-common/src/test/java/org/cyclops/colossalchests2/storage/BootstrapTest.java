package org.cyclops.colossalchests2.storage;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.BeforeClass;

/**
 * Base class for tests that need vanilla registries.
 * @author rubensworks
 */
public abstract class BootstrapTest {

    private static boolean componentsBound;

    @BeforeClass
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Item components are normally bound when a world loads its registries.
        if (!componentsBound) {
            componentsBound = true;
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
        }
    }

}
