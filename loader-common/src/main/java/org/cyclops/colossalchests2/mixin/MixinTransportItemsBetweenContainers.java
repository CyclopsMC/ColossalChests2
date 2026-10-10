package org.cyclops.colossalchests2.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.cyclops.colossalchests2.golem.GolemTransport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lets copper golems use colossal chests, see {@link GolemTransport}. Loaders have no hook for golem targets.
 * @author rubensworks
 */
@Mixin(value = TransportItemsBetweenContainers.class, remap = false)
public class MixinTransportItemsBetweenContainers {

    @Shadow
    @Final
    private float speedModifier;

    @Shadow
    private TransportItemsBetweenContainers.TransportItemTarget target;

    @Shadow
    private static double getInteractionRange(PathfinderMob body) {
        throw new AssertionError();
    }

    @Shadow
    private Vec3 getPositionToReachTargetFrom(Path path, PathfinderMob body) {
        throw new AssertionError();
    }

    @Shadow
    private boolean isWithinTargetDistance(double distance, TransportItemsBetweenContainers.TransportItemTarget target, Level level,
                                           PathfinderMob body, Vec3 fromPos) {
        throw new AssertionError();
    }

    @Shadow
    private boolean targetIsReachableFromPosition(Level level, boolean canReachTarget, Vec3 pos,
                                                  TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob body) {
        throw new AssertionError();
    }

    @Shadow
    private static Set<GlobalPos> getVisitedPositions(PathfinderMob mob) {
        throw new AssertionError();
    }

    @Shadow
    private static Set<GlobalPos> getUnreachablePositions(PathfinderMob mob) {
        throw new AssertionError();
    }

    @Shadow
    private AABB getTargetSearchArea(PathfinderMob mob) {
        throw new AssertionError();
    }

    @Shadow
    private int getHorizontalSearchDistance(PathfinderMob mob) {
        throw new AssertionError();
    }

    @Shadow
    private TransportItemsBetweenContainers.TransportItemTarget isTargetValidToPick(PathfinderMob body, Level level, BlockEntity blockEntity,
            Set<GlobalPos> visitedPositions, Set<GlobalPos> unreachablePositions, AABB targetBlockSearchArea) {
        throw new AssertionError();
    }

    // Vanilla only scans chest block entities, so add the closest colossal chest target if it is closer.
    @Inject(method = "getTransportTarget", at = @At("RETURN"), cancellable = true)
    private void colossalchests2$addChestTargets(ServerLevel level, PathfinderMob body,
                                                 CallbackInfoReturnable<Optional<TransportItemsBetweenContainers.TransportItemTarget>> cir) {
        if (!(body instanceof CopperGolem)) {
            return;
        }
        boolean pickingUp = body.getMainHandItem().isEmpty();
        TransportItemsBetweenContainers.TransportItemTarget closest = cir.getReturnValue().orElse(null);
        double closestDistance = closest == null ? Float.MAX_VALUE : closest.pos().distToCenterSqr(body.position());
        TransportItemsBetweenContainers.TransportItemTarget initial = closest;
        AABB area = getTargetSearchArea(body);
        Set<GlobalPos> visited = getVisitedPositions(body);
        Set<GlobalPos> unreachable = getUnreachablePositions(body);
        List<ChunkPos> chunks = ChunkPos.rangeClosed(ChunkPos.containing(body.blockPosition()),
                Math.floorDiv(getHorizontalSearchDistance(body), 16) + 1).toList();
        for (ChunkPos chunkPos : chunks) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk != null) {
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    double distance = blockEntity.getBlockPos().distToCenterSqr(body.position());
                    if (distance < closestDistance && GolemTransport.isTarget(blockEntity, pickingUp)) {
                        TransportItemsBetweenContainers.TransportItemTarget target = isTargetValidToPick(body, level, blockEntity, visited, unreachable, area);
                        if (target != null) {
                            closest = target;
                            closestDistance = distance;
                        }
                    }
                }
            }
        }
        if (closest != initial) {
            cir.setReturnValue(Optional.of(closest));
        }
    }

    @Inject(method = "isWantedBlock", at = @At("HEAD"), cancellable = true)
    private void colossalchests2$isWantedBlock(PathfinderMob mob, BlockState block, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof CopperGolem && (mob.getMainHandItem().isEmpty() ? GolemTransport.isSource(block) : GolemTransport.isDestination(block))) {
            cir.setReturnValue(true);
        }
    }

    // Colossal chests open no matter what is on top of them.
    @Inject(method = "isTargetBlocked", at = @At("HEAD"), cancellable = true)
    private void colossalchests2$isTargetBlocked(Level level, TransportItemsBetweenContainers.TransportItemTarget target, CallbackInfoReturnable<Boolean> cir) {
        if (target.container() instanceof GolemTransport.ChestContainer) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "walkTowardsTarget", at = @At("HEAD"), cancellable = true)
    private void colossalchests2$walkTowardsTarget(PathfinderMob body, CallbackInfo ci) {
        BlockPos approach = colossalchests2$getApproachPos(this.target);
        if (approach != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(body, approach, this.speedModifier, 0);
            ci.cancel();
        }
    }

    // Like vanilla, but with a path to the side of the target, see walkTowardsTarget.
    @Inject(method = "hasValidTravellingPath", at = @At("HEAD"), cancellable = true)
    private void colossalchests2$hasValidTravellingPath(Level level, TransportItemsBetweenContainers.TransportItemTarget target, PathfinderMob body,
                                                        CallbackInfoReturnable<Boolean> cir) {
        BlockPos approach = colossalchests2$getApproachPos(target);
        if (approach != null) {
            Path path = body.getNavigation().getPath() == null ? body.getNavigation().createPath(approach, 0) : body.getNavigation().getPath();
            Vec3 from = getPositionToReachTargetFrom(path, body);
            boolean canReachTarget = isWithinTargetDistance(getInteractionRange(body), target, level, body, from);
            cir.setReturnValue((path == null && !canReachTarget) || targetIsReachableFromPosition(level, canReachTarget, from, target, body));
        }
    }

    @Inject(method = "isWithinTargetDistance", at = @At("HEAD"), cancellable = true)
    private void colossalchests2$isWithinTargetDistance(double distance, TransportItemsBetweenContainers.TransportItemTarget target, Level level,
                                                        PathfinderMob body, Vec3 fromPos, CallbackInfoReturnable<Boolean> cir) {
        if (target.container() instanceof GolemTransport.ChestContainer chest) {
            AABB box = body.getBoundingBox();
            AABB moved = AABB.ofSize(fromPos, box.getXsize(), box.getYsize(), box.getZsize());
            cir.setReturnValue(GolemTransport.getReachArea(chest, target.pos()).inflate(distance, 0.5, distance).intersects(moved));
        }
    }

    // Rays to the faces of a target above the golem graze it or hit the wall below, so check the space in front.
    @Inject(method = "canSeeAnyTargetSide", at = @At("HEAD"), cancellable = true)
    private void colossalchests2$canSeeAnyTargetSide(TransportItemsBetweenContainers.TransportItemTarget target, Level level, PathfinderMob body,
                                                     Vec3 eyePosition, CallbackInfoReturnable<Boolean> cir) {
        BlockPos approach = colossalchests2$getApproachPos(target);
        if (approach != null) {
            ClipContext context = new ClipContext(eyePosition, Vec3.atCenterOf(approach), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body);
            cir.setReturnValue(level.clip(context).getType() == HitResult.Type.MISS);
        }
    }

    private static BlockPos colossalchests2$getApproachPos(TransportItemsBetweenContainers.TransportItemTarget target) {
        return target != null && target.container() instanceof GolemTransport.ChestContainer chest
                ? GolemTransport.getApproachPos(chest, target.pos()) : null;
    }

    @Inject(method = "matchesGettingItemsRequirement", at = @At("HEAD"), cancellable = true)
    private static void colossalchests2$matchesGettingItemsRequirement(Container container, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof GolemTransport.ChestContainer chest) {
            cir.setReturnValue(chest.isSource() && GolemTransport.canPickUp(chest));
        }
    }

    @Inject(method = "matchesLeavingItemsRequirement", at = @At("HEAD"), cancellable = true)
    private static void colossalchests2$matchesLeavingItemsRequirement(PathfinderMob body, Container container, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof GolemTransport.ChestContainer chest) {
            cir.setReturnValue(!chest.isSource() && GolemTransport.canPutDown(body, chest));
        }
    }

    @Inject(method = "pickupItemFromContainer", at = @At("HEAD"), cancellable = true)
    private static void colossalchests2$pickupItemFromContainer(Container container, CallbackInfoReturnable<ItemStack> cir) {
        if (container instanceof GolemTransport.ChestContainer chest) {
            cir.setReturnValue(GolemTransport.pickUp(chest));
        }
    }

    @Inject(method = "addItemsToContainer", at = @At("HEAD"), cancellable = true)
    private static void colossalchests2$addItemsToContainer(PathfinderMob body, Container container, CallbackInfoReturnable<ItemStack> cir) {
        if (container instanceof GolemTransport.ChestContainer chest) {
            cir.setReturnValue(GolemTransport.putDown(body, chest));
        }
    }
}
