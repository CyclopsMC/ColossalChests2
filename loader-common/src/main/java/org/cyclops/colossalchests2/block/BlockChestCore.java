package org.cyclops.colossalchests2.block;

import java.util.function.Consumer;
import net.minecraft.world.level.redstone.Orientation;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cyclops.colossalchests2.RegistryEntries;
import org.cyclops.colossalchests2.api.ChestMaterial;
import org.cyclops.colossalchests2.api.block.IChestMember;
import org.cyclops.colossalchests2.blockentity.BlockEntityChestCore;
import org.cyclops.colossalchests2.upgrade.UpgradeSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * The core of a chest structure, which holds all contents.
 * Broken cores keep their contents as item data, so nothing is ever ejected into the world.
 * @author rubensworks
 */
public class BlockChestCore extends BaseEntityBlock implements ITooltipBlock {

    public static final BooleanProperty FORMED = IChestMember.FORMED;

    private static final List<BlockChestCore> INSTANCES = Lists.newArrayList();

    private final ChestMaterial material;

    public BlockChestCore(Properties properties, ChestMaterial material) {
        super(properties);
        this.material = material;
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
        INSTANCES.add(this);
    }

    public static List<BlockChestCore> getInstances() {
        return Collections.unmodifiableList(INSTANCES);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(material.getLimitsTooltip());
        UpgradeSet upgrades = UpgradeSet.of(stack.getOrDefault(RegistryEntries.COMPONENT_CHEST_UPGRADES.value(), ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().toList());
        if (!upgrades.counts().isEmpty()) {
            tooltip.accept(Component.translatable("block.colossalchests2.chest_core.upgrades").withStyle(ChatFormatting.GRAY));
            upgrades.counts().forEach((upgrade, count) -> tooltip.accept(Component.translatable("block.colossalchests2.chest_core.upgrade",
                    count, upgrade.getDisplayName()).withStyle(ChatFormatting.GRAY)));
        }
    }

    public ChestMaterial getMaterial() {
        return material;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    /**
     * Formed members are drawn by the core's giant chest. Their formed model is invisible,
     * but it is still a model so vanilla draws the breaking crack on it.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ChestInteractions.useItemOn(stack, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ChestInteractions.use(state, level, pos, player);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return RegistryEntries.BLOCK_ENTITY_CHEST_CORE.value().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, RegistryEntries.BLOCK_ENTITY_CHEST_CORE.value(),
                level.isClientSide() ? BlockEntityChestCore::clientTick : BlockEntityChestCore::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof BlockEntityChestCore core) {
            core.requestValidation();
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Creative mode skips drops, but a core with contents or upgrades must never vanish.
        if (!level.isClientSide() && player.isCreative() && level.getBlockEntity(pos) instanceof BlockEntityChestCore core
                && (!core.getStorage().toContents().entries().isEmpty() || !core.getUpgrades().isEmpty())) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(core.collectComponents());
            popResourceFromFace(level, pos, getFaceTowards(pos, player), stack);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        // Like the default, but drops pop out on the player's side, so they cannot fall into the chest interior.
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        Direction face = getFaceTowards(pos, player);
        for (ItemStack drop : getDrops(state, level, pos, blockEntity, player, tool)) {
            popResourceFromFace(level, pos, face, drop);
        }
        state.spawnAfterBreak(level, pos, tool, true);
    }

    /**
     * @return The side of the block closest to the player's eyes.
     */
    public static Direction getFaceTowards(BlockPos pos, Player player) {
        Vec3 offset = player.getEyePosition().subtract(Vec3.atCenterOf(pos));
        return Direction.getApproximateNearest(offset.x, offset.y, offset.z);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof BlockEntityChestCore core ? core.getComparatorSignal() : 0;
    }
}
