package org.cyclops.colossalchests2.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.cyclops.colossalchests2.api.IChest;

/**
 * Shows a functional wall's icon on its outer faces of the giant chest while the player sneaks, like the core marker.
 * @author rubensworks
 */
public class FunctionalWallOverlay implements IChestOverlay {

    private static final float MIN = 3F / 16F;
    private static final float MAX = 13F / 16F;

    private final Identifier texture;

    /**
     * @param texture A block atlas texture, such as othermod:block/chest_wall_example_icon.
     */
    public FunctionalWallOverlay(Identifier texture) {
        this.texture = texture;
    }

    @Override
    public void render(IChest chest, BlockPos pos, Direction face, float partialTick,
                       PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
        if (!ChestOverlayHelpers.isRevealingMembers()) {
            return;
        }
        ChestOverlayHelpers.renderSprite(poseStack, collector, ChestOverlayHelpers.getBlockSprite(texture),
                MIN, MIN, MAX, MAX, light, overlay);
    }

}
