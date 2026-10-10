package org.cyclops.colossalchests2.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.cyclops.colossalchests2.inventory.ContainerRedstone;

import java.util.List;

/**
 * Settings of a Redstone wall.
 * @author rubensworks
 */
public class ContainerScreenRedstone extends AbstractContainerScreen<ContainerRedstone> {

    private static final int COLOR_LABEL = 0xFF404040;
    private static final int COLOR_SIGNAL = 0xFFC02010;

    public ContainerScreenRedstone(ContainerRedstone menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ContainerRedstone.WIDTH, ContainerRedstone.HEIGHT);
        this.inventoryLabelY = ContainerRedstone.INVENTORY_Y - 11;
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
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        // What the slot means when empty, and the signal it gives now.
        Component target = menu.getSlot(0).hasItem() ? menu.getSlot(0).getItem().getHoverName()
                : Component.translatable("gui.colossalchests2.redstone.whole_chest");
        int textX = ContainerRedstone.TARGET_X + 22;
        guiGraphics.text(font, target, textX, ContainerRedstone.TARGET_Y - 1, COLOR_LABEL, false);
        Component signal = Component.translatable("gui.colossalchests2.redstone.signal", menu.getSignal());
        guiGraphics.text(font, signal, textX, ContainerRedstone.TARGET_Y + 9, COLOR_SIGNAL, false);
    }


    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && menu.isGhostSlot(hoveredSlot.index) && !hoveredSlot.hasItem()) {
            String key = "gui.colossalchests2.redstone.whole_chest";
            guiGraphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(key),
                    Component.translatable(key + ".info").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        super.extractTooltip(guiGraphics, mouseX, mouseY);
    }
}
