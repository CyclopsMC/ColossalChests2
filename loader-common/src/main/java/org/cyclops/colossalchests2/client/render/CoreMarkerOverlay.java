package org.cyclops.colossalchests2.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.cyclops.colossalchests2.api.ChestMaterial;
import org.cyclops.colossalchests2.api.IChest;
import org.cyclops.colossalchests2.api.client.ChestOverlayHelpers;
import org.cyclops.colossalchests2.api.client.IChestOverlay;

/**
 * Marks where the core of a formed chest is, while the player sneaks.
 * @author rubensworks
 */
public class CoreMarkerOverlay implements IChestOverlay {

    public static final CoreMarkerOverlay INSTANCE = new CoreMarkerOverlay();

    private static final float MIN = 5F / 16F;
    private static final float MAX = 11F / 16F;

    @Override
    public void render(IChest chest, BlockPos pos, Direction face, float partialTick,
                       PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
        ChestMaterial material = chest.getChestMaterial();
        if (material == null || !isVisible()) {
            return;
        }
        Identifier texture = material.id().withPrefix("block/chest_core_");
        ChestOverlayHelpers.renderSprite(poseStack, collector, ChestOverlayHelpers.getBlockSprite(texture),
                MIN, MIN, MAX, MAX, light, overlay);
    }

    protected boolean isVisible() {
        return ChestOverlayHelpers.isRevealingMembers();
    }

}
