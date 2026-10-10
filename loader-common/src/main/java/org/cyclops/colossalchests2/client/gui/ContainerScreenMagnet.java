package org.cyclops.colossalchests2.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.cyclops.colossalchests2.inventory.ContainerMagnet;

/**
 * Settings of a Magnet wall: its radius.
 * @author rubensworks
 */
public class ContainerScreenMagnet extends AbstractContainerScreen<ContainerMagnet> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 64;
    private static final int BUTTON_Y = 22;
    private static final int BUTTON_SIZE = 20;
    private static final int COLOR_LABEL = 0xFF404040;
    private static final int COLOR_HINT = 0xFF707070;

    public ContainerScreenMagnet(ContainerMagnet menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        Tooltip hint = Tooltip.create(Component.translatable("gui.colossalchests2.magnet.radius.step", ContainerMagnet.STEP_MORE));
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(ContainerMagnet.BUTTON_DECREASE, ContainerMagnet.BUTTON_DECREASE_MORE))
                .bounds(leftPos + 8, topPos + BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE).tooltip(hint).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(ContainerMagnet.BUTTON_INCREASE, ContainerMagnet.BUTTON_INCREASE_MORE))
                .bounds(leftPos + WIDTH - 8 - BUTTON_SIZE, topPos + BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE).tooltip(hint).build());
    }

    private void click(int button, int buttonMore) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, minecraft.hasShiftDown() ? buttonMore : button);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        GuiPanels.drawPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(font, title, titleLabelX, titleLabelY, COLOR_LABEL, false);
        Component radius = Component.translatable("gui.colossalchests2.magnet.radius", menu.getRadius());
        guiGraphics.text(font, radius, (WIDTH - font.width(radius)) / 2, BUTTON_Y + 6, COLOR_LABEL, false);
        Component max = Component.translatable("gui.colossalchests2.magnet.radius.max", menu.getMaxRadius());
        guiGraphics.text(font, max, (WIDTH - font.width(max)) / 2, BUTTON_Y + BUTTON_SIZE + 6, COLOR_HINT, false);
    }

}
