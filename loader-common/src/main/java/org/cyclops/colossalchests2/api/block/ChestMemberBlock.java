package org.cyclops.colossalchests2.api.block;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cyclops.colossalchests2.api.ColossalChestsApi;

/**
 * A block that can be part of a chest's shell in any material, such as a functional wall.
 * It has the {@link #FORMED} state, makes nearby chests check their structure when it is placed, removed or
 * its surroundings change, and opens its chest when right-clicked.
 * @author rubensworks
 */
public class ChestMemberBlock extends Block implements IChestMember {

    public ChestMemberBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return IChestMember.getFormedOcclusionShape(state, super.getOcclusionShape(state));
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return IChestMember.getFormedLightBlock(state, super.getLightDampening(state));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return IChestMember.getFormedPropagatesSkylightDown(state, super.propagatesSkylightDown(state));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ColossalChestsApi.get().useItemOnMember(stack, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ColossalChestsApi.get().useMember(state, level, pos, player);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            ColossalChestsApi.get().requestValidationNear(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        ColossalChestsApi.get().requestValidationNear(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        ColossalChestsApi.get().requestValidationNear(level, pos);
    }

}
