package org.cyclops.colossalchests2.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.cyclops.colossalchests2.blockentity.BlockEntityUncolossalChest;
import org.jetbrains.annotations.Nullable;

/**
 * Renders an uncolossal chest as a scaled down vanilla chest.
 * @author rubensworks
 */
public class RenderUncolossalChest implements BlockEntityRenderer<BlockEntityUncolossalChest, RenderUncolossalChest.State> {

    private static final float SCALE = 0.3375F;

    private final ChestModel model;
    private final SpriteGetter sprites;
    private final boolean christmas = ChestRenderer.xmasTextures();

    public RenderUncolossalChest(BlockEntityRendererProvider.Context context) {
        this.model = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntityUncolossalChest chest, State state, float partialTick, Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTick, cameraPosition, breakProgress);
        float openness = 1.0F - chest.getOpenNess(partialTick);
        state.openness = 1.0F - openness * openness * openness;
        state.facing = chest.getFacing();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0, 0.5F);
        poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-0.5F, 0, -0.5F);
        SpriteId material = Sheets.chooseSprite(christmas ? ChestRenderState.ChestMaterialType.CHRISTMAS
                : ChestRenderState.ChestMaterialType.REGULAR, ChestType.SINGLE);
        collector.submitModel(model, state.openness, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, material, sprites, 0);
        if (state.breakProgress != null) {
            collector.order(1).submitCrumblingOverlay(model, state.openness, poseStack, material.renderType(RenderTypes::entityCutout),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
        }
        poseStack.popPose();
    }

    public static class State extends BlockEntityRenderState {
        public float openness;
        public Direction facing = Direction.NORTH;
    }

}
