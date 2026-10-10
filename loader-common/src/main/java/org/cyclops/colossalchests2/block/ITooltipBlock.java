package org.cyclops.colossalchests2.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * A block that adds lines to the tooltip of its item, shown through {@link ItemBlockTooltip}.
 * @author rubensworks
 */
public interface ITooltipBlock {

    void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag);

}
