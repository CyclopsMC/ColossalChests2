package org.cyclops.colossalchests2.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Item of an {@link ITooltipBlock}, since blocks no longer add to tooltips themselves.
 * @author rubensworks
 */
public class ItemBlockTooltip extends BlockItem {

    public ItemBlockTooltip(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (getBlock() instanceof ITooltipBlock tooltipBlock) {
            tooltipBlock.appendTooltip(stack, context, tooltip, flag);
        }
    }

    public static <M extends IModBase> BiFunction<BlockConfigCommon<M>, Block, ? extends BlockItem> constructor() {
        return (eConfig, block) -> new ItemBlockTooltip(block, eConfig.createDefaultItemProperties());
    }

}
