package org.cyclops.colossalchests2.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Drawing helpers for {@link IChestOverlay}s, in the face's unit square.
 * @author rubensworks
 */
public final class ChestOverlayHelpers {

    private ChestOverlayHelpers() {
    }

    /**
     * @param texture A block texture, such as othermod:block/example.
     * @return Its sprite on the block atlas.
     */
    public static TextureAtlasSprite getBlockSprite(Identifier texture) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(texture);
    }

    /**
     * @param texture An item texture, such as othermod:item/example.
     * @return Its sprite on the item atlas.
     */
    public static TextureAtlasSprite getItemSprite(Identifier texture) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).getSprite(texture);
    }

    /**
     * Draw a whole atlas sprite, stretched over an area.
     * @param poseStack The pose stack.
     * @param collector Collects what to draw.
     * @param sprite A sprite, such as from {@link #getBlockSprite(Identifier)}.
     * @param x0 Left edge.
     * @param y0 Bottom edge.
     * @param x1 Right edge.
     * @param y1 Top edge.
     * @param light The packed light.
     * @param overlay The packed overlay.
     */
    public static void renderSprite(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite,
                                    float x0, float y0, float x1, float y1, int light, int overlay) {
        renderSprite(poseStack, collector, sprite, x0, y0, x1, y1, light, overlay, 0xFFFFFFFF);
    }

    /**
     * {@link #renderSprite(PoseStack, SubmitNodeCollector, TextureAtlasSprite, float, float, float, float, int, int)},
     * tinted.
     * @param color The ARGB tint.
     */
    public static void renderSprite(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite,
                                    float x0, float y0, float x1, float y1, int light, int overlay, int color) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        // Texture v grows downwards, face y grows upwards.
        float v0 = sprite.getV1();
        float v1 = sprite.getV0();
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(sprite.atlasLocation()), (pose, buffer) -> {
            vertex(buffer, pose, x0, y0, u0, v0, light, overlay, color);
            vertex(buffer, pose, x1, y0, u1, v0, light, overlay, color);
            vertex(buffer, pose, x1, y1, u1, v1, light, overlay, color);
            vertex(buffer, pose, x0, y1, u0, v1, light, overlay, color);
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int light, int overlay, int color) {
        buffer.addVertex(pose, x, y, 0)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0, 0, 1);
    }

    /**
     * Draw the four sides of the face's unit square back to the chest surface, so a full face overlay looks like a
     * plate on the chest instead of floating in front of it.
     * @param poseStack The pose stack.
     * @param collector Collects what to draw.
     * @param sprite A sprite, whose outer rows texture the sides.
     * @param depth How far the face lies in front of the chest surface.
     * @param light The packed light.
     * @param overlay The packed overlay.
     */
    public static void renderSides(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, float depth,
                                   int light, int overlay) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = v0 + (sprite.getV1() - v0) * Math.min(1F, depth);
        float z = -depth;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(sprite.atlasLocation()), (pose, buffer) -> {
            // Bottom, top, left, right, each counter-clockwise seen from outside.
            quad(buffer, pose, 0, -1, 0, light, overlay, 0, 0, z, u0, v1, 1, 0, z, u1, v1, 1, 0, 0, u1, v0, 0, 0, 0, u0, v0);
            quad(buffer, pose, 0, 1, 0, light, overlay, 0, 1, 0, u0, v0, 1, 1, 0, u1, v0, 1, 1, z, u1, v1, 0, 1, z, u0, v1);
            quad(buffer, pose, -1, 0, 0, light, overlay, 0, 0, 0, u0, v0, 0, 1, 0, u1, v0, 0, 1, z, u1, v1, 0, 0, z, u0, v1);
            quad(buffer, pose, 1, 0, 0, light, overlay, 1, 0, z, u0, v1, 1, 1, z, u1, v1, 1, 1, 0, u1, v0, 1, 0, 0, u0, v0);
        });
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float nx, float ny, float nz, int light, int overlay,
                             float... vertices) {
        for (int i = 0; i < 20; i += 5) {
            buffer.addVertex(pose, vertices[i], vertices[i + 1], vertices[i + 2])
                    .setColor(0xFFFFFFFF)
                    .setUv(vertices[i + 3], vertices[i + 4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    /**
     * Draw an item flat on the face, like in a GUI slot.
     * @param poseStack The pose stack.
     * @param collector Collects what to draw.
     * @param level The level.
     * @param stack The item.
     * @param centerX Horizontal center.
     * @param centerY Vertical center.
     * @param size Width and height.
     * @param light The packed light.
     */
    public static void renderItem(PoseStack poseStack, SubmitNodeCollector collector, Level level, ItemStack stack,
                                  float centerX, float centerY, float size, int light) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        // Flatten towards the face, so 3D block items do not stick out of the chest.
        poseStack.scale(size, size, 0.001F);
        getItemRenderState(level, stack, ItemDisplayContext.GUI).submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /**
     * @param level The level.
     * @param stack The item.
     * @param displayContext How the item is shown.
     * @return What to draw for the item.
     */
    public static ItemStackRenderState getItemRenderState(Level level, ItemStack stack, ItemDisplayContext displayContext) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, displayContext, level, null, 0);
        return state;
    }

    /**
     * @return If the player sneaks, which reveals the hidden members of a chest.
     */
    public static boolean isRevealingMembers() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCrouching();
    }

    /**
     * Draw a line of text with a shadow on the face, centered horizontally.
     * @param poseStack The pose stack.
     * @param collector Collects what to draw.
     * @param text The text.
     * @param centerX Horizontal center.
     * @param y0 Bottom edge.
     * @param height Height of the text.
     * @param color The RGB color.
     * @param light The packed light.
     */
    public static void renderText(PoseStack poseStack, SubmitNodeCollector collector, String text, float centerX, float y0, float height,
                                  int color, int light) {
        Font font = Minecraft.getInstance().font;
        float scale = height / font.lineHeight;
        poseStack.pushPose();
        // Font y grows downwards, face y upwards.
        poseStack.translate(centerX, y0 + height, 0);
        poseStack.scale(scale, -scale, scale);
        collector.submitText(poseStack, -font.width(text) / 2F, 0, Component.literal(text).getVisualOrderText(), true,
                Font.DisplayMode.POLYGON_OFFSET, light, 0xFF000000 | color, 0, 0);
        poseStack.popPose();
    }

}
