package org.cyclops.colossalchests2.client.render;

import net.minecraft.util.LightCoordsUtil;
import com.google.common.collect.Lists;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.cyclops.colossalchests2.api.ChestMaterial;
import org.cyclops.colossalchests2.api.client.ChestOverlays;
import org.cyclops.colossalchests2.api.client.IChestOverlay;
import org.cyclops.colossalchests2.block.BlockChestCore;
import org.cyclops.colossalchests2.blockentity.BlockEntityChestCore;
import org.cyclops.colossalchests2.multiblock.ChestShape;
import org.cyclops.colossalchests2.multiblock.ChestStructure;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Renders a formed chest as one giant vanilla-style chest whose body exactly fills the structure,
 * followed by the {@link IChestOverlay}s of its decorated members.
 * @author rubensworks
 */
public class RenderChestCore implements BlockEntityRenderer<BlockEntityChestCore, RenderChestCore.State> {

    /**
     * How far overlays float in front of the chest surface, far enough to avoid z-fighting.
     * The lock sticks out further, so it hides the part of an overlay right behind it, like any object in front.
     */
    public static final float OVERLAY_OFFSET = 1F / 32F;

    private static final float MODEL_HEIGHT = 14F / 16F;
    private static final float LID_PIVOT_Y = 9F / 16F;
    private static final float LID_PIVOT_Z = 1F / 16F;

    private final ChestModel model;
    private final SpriteGetter sprites;
    private final boolean christmas = ChestRenderer.xmasTextures();

    public RenderChestCore(BlockEntityRendererProvider.Context context) {
        this.model = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntityChestCore core, State state, float partialTick, Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        // The break progress of the core block would cover the whole giant chest, so it is left out.
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPosition, null);
        state.core = core;
        state.partialTick = partialTick;
        state.overlays.clear();
        ChestStructure structure = core.getStructure();
        Level level = core.getLevel();
        if (structure == null || level == null || !(core.getBlockState().getBlock() instanceof BlockChestCore block)) {
            state.structure = null;
            return;
        }
        state.structure = structure;
        state.material = block.getMaterial();
        state.facing = core.getFacing();
        float openness = 1.0F - core.getOpenness(partialTick);
        state.openness = 1.0F - openness * openness * openness;
        state.lidRotation = -(state.openness * ((float) Math.PI / 2F));
        state.bodyLight = LightCoordsUtil.getLightCoords(level, ChestShape.getFrontPos(structure, state.facing));
        for (BlockPos pos : core.getDecoratedPositions()) {
            Block member = level.getBlockState(pos).getBlock();
            IChestOverlay chestOverlay = member instanceof BlockChestCore ? CoreMarkerOverlay.INSTANCE : ChestOverlays.get(member);
            if (chestOverlay == null) {
                continue;
            }
            for (Direction face : ChestShape.getOuterFaces(structure, pos)) {
                state.overlays.add(new OverlayFace(pos, face, chestOverlay, LightCoordsUtil.getLightCoords(level, pos.relative(face))));
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.structure == null) {
            return;
        }
        poseStack.pushPose();
        applyChestTransform(poseStack, state.blockPos, state.structure, state.facing);
        SpriteId material = getMaterial(state.material);
        collector.submitModel(model, state.openness, poseStack, state.bodyLight, OverlayTexture.NO_OVERLAY, -1, material, sprites, 0);
        poseStack.popPose();

        BlockPos origin = state.blockPos;
        for (OverlayFace overlayFace : state.overlays) {
            BlockPos pos = overlayFace.pos();
            poseStack.pushPose();
            if (state.lidRotation != 0 && ChestShape.isOnLid(state.structure, pos)) {
                applyLidTransform(poseStack, origin, state.structure, state.facing, state.lidRotation);
            }
            poseStack.translate(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
            applyFaceTransform(poseStack, overlayFace.face(), state.facing);
            overlayFace.overlay().render(state.core, pos, overlayFace.face(), state.partialTick, poseStack, collector,
                    overlayFace.light(), OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /**
     * Map the vanilla chest model space onto the structure, relative to the core.
     * The model body spans 14 pixels in each direction, which becomes the structure size.
     */
    public static void applyChestTransform(PoseStack poseStack, BlockPos origin, ChestStructure structure, Direction facing) {
        float scale = structure.size() / MODEL_HEIGHT;
        poseStack.translate(
                structure.min().getX() - origin.getX() + structure.size() / 2F,
                structure.min().getY() - origin.getY(),
                structure.min().getZ() - origin.getZ() + structure.size() / 2F);
        poseStack.rotateDegrees(Axis.YP, -facing.toYRot());
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, 0, -0.5F);
    }

    /**
     * Rotate around the lid hinge in core-relative space, so things on the lid move along with it.
     */
    public static void applyLidTransform(PoseStack poseStack, BlockPos origin, ChestStructure structure, Direction facing, float lidRotation) {
        float scale = structure.size() / MODEL_HEIGHT;
        applyChestTransform(poseStack, origin, structure, facing);
        poseStack.translate(0, LID_PIVOT_Y, LID_PIVOT_Z);
        poseStack.rotate(Axis.XP, lidRotation);
        poseStack.translate(0, -LID_PIVOT_Y, -LID_PIVOT_Z);
        // Undo the chest transform.
        poseStack.translate(0.5F, 0, 0.5F);
        poseStack.scale(1 / scale, 1 / scale, 1 / scale);
        poseStack.rotateDegrees(Axis.YP, facing.toYRot());
        poseStack.translate(
                -(structure.min().getX() - origin.getX() + structure.size() / 2F),
                -(structure.min().getY() - origin.getY()),
                -(structure.min().getZ() - origin.getZ() + structure.size() / 2F));
    }

    /**
     * Map a block's unit cube onto the face-local unit square of {@link IChestOverlay}.
     */
    public static void applyFaceTransform(PoseStack poseStack, Direction face, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (face.getAxis().isHorizontal()) {
            poseStack.rotateDegrees(Axis.YP, -face.toYRot());
        } else {
            poseStack.rotateDegrees(Axis.YP, -facing.toYRot());
            poseStack.rotateDegrees(Axis.XP, face == Direction.UP ? -90 : 90);
        }
        poseStack.translate(-0.5F, -0.5F, 0.5F + OVERLAY_OFFSET);
    }

    protected SpriteId getMaterial(ChestMaterial material) {
        if (ChestMaterial.WOOD.equals(material)) {
            return Sheets.chooseSprite(christmas ? ChestRenderState.ChestMaterialType.CHRISTMAS : ChestRenderState.ChestMaterialType.REGULAR,
                    ChestType.SINGLE);
        }
        return new SpriteId(Sheets.CHEST_SHEET, material.id().withPrefix("entity/chest/"));
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    public static class State extends BlockEntityRenderState {
        public BlockEntityChestCore core;
        public float partialTick;
        @Nullable
        public ChestStructure structure;
        public ChestMaterial material;
        public Direction facing = Direction.NORTH;
        public float openness;
        public float lidRotation;
        public int bodyLight;
        public final List<OverlayFace> overlays = Lists.newArrayList();
    }

    /**
     * An outer face of a decorated member, with what to draw on it.
     */
    public record OverlayFace(BlockPos pos, Direction face, IChestOverlay overlay, int light) {
    }

}
