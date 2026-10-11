package org.cyclops.colossalchests2.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.cyclops.colossalchests2.golem.GolemTransport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives copper golems a container for colossal chests, see {@link GolemTransport}.
 * @author rubensworks
 */
@Mixin(value = TransportItemsBetweenContainers.TransportItemTarget.class, remap = false)
public class MixinTransportItemTarget {

    @Inject(method = "getBlockEntityContainer", at = @At("HEAD"), cancellable = true)
    private static void colossalchests2$getBlockEntityContainer(BlockEntity blockEntity, BlockState blockState, Level level, BlockPos blockPos,
                                                                CallbackInfoReturnable<Container> cir) {
        Container container = GolemTransport.getContainer(blockEntity);
        if (container != null) {
            cir.setReturnValue(container);
        }
    }
}
