package org.cyclops.colossalchests2.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.cyclops.colossalchests2.capability.WallAccess;
import org.cyclops.colossalchests2.inventory.ContainerInterface;

import java.util.List;

/**
 * Settings of an Interface.
 * @author rubensworks
 */
public class ContainerScreenInterface extends AbstractContainerScreen<ContainerInterface> {

    private static final int MODE_BUTTON_X = 7;
    private static final int MODE_BUTTON_WIDTH = 82;

    private Button modeButton;
    private WallAccess.Mode shownMode;

    public ContainerScreenInterface(ContainerInterface menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ContainerInterface.WIDTH, ContainerInterface.HEIGHT);
        this.inventoryLabelY = ContainerInterface.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(getModeLabel(menu.getMode()),
                        button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ContainerInterface.BUTTON_MODE))
                .bounds(leftPos + MODE_BUTTON_X, topPos + ContainerInterface.MODE_Y, MODE_BUTTON_WIDTH, 20)
                .build());
        shownMode = menu.getMode();
    }

    private static Component getModeLabel(WallAccess.Mode mode) {
        return Component.translatable(mode.getTranslationKey());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // The mode is synced after the click, so the label follows.
        if (menu.getMode() != shownMode) {
            shownMode = menu.getMode();
            modeButton.setMessage(getModeLabel(shownMode));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        GuiPanels.drawPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            GuiPanels.drawSlot(guiGraphics, leftPos + slot.x, topPos + slot.y);
        }
    }


    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && menu.isGhostSlot(hoveredSlot.index) && !hoveredSlot.hasItem()) {
            String key = "gui.colossalchests2.wall.filter";
            guiGraphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(key),
                    Component.translatable(key + ".info").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        if (modeButton.isHovered()) {
            guiGraphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.colossalchests2.wall.mode"),
                    Component.translatable(menu.getMode().getTranslationKey() + ".info").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        super.extractTooltip(guiGraphics, mouseX, mouseY);
    }
}
