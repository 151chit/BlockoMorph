package net.blockomorph.mixins.main;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.MovementAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityInsideSupportMixin {
	@Shadow private Level level;
	@Shadow protected abstract boolean isAffectedByBlocks();
	@Shadow protected abstract void onInsideBlock(BlockState blockState);
	@Shadow protected abstract AABB makeBoundingBox(Vec3 vec3);

	@Shadow protected abstract boolean collidedWithShapeMovingFrom(Vec3 vec3, Vec3 vec32, List<AABB> list);

	@Shadow @Final private LongSet visitedBlocks;

	@Inject(require = 1, at = @At(value = "TAIL"), method = "checkInsideBlocks")
	private void checkInsideBlocks(List<?> list, InsideBlockEffectApplier.StepBasedCollector stepBasedCollector, CallbackInfo ci) {
		if (Config.get().entityInside.getValue() && this.isAffectedByBlocks()) {
			for (Object obj : list) {
				if (obj instanceof MovementAccessor acc) {
					Vec3 from = acc.getMovement$blockomorph()[0];
					Vec3 to = acc.getMovement$blockomorph()[1];
					AABB entityBox = this.makeBoundingBox(to).deflate(1.0E-5F);
					Entity self = (Entity) (Object) this;
					for (PlayerAccessor pl : PlayersMultiSectionStorage.fromLevel(this.level).findMorphed(self, entityBox)) {
						pl.getBlocksData2InArea(entityBox, (pos, block, realPos) -> {
							BlockState blockState = block.getBlockState();
							if (!blockState.isAir() && this.visitedBlocks.add(block.getPos().asLong())) {
								VoxelShape voxelShape = blockState.getEntityInsideCollisionShape(this.level, block.getPos(), self);
								boolean bl = voxelShape == Shapes.block() || this.collidedWithShapeMovingFrom(from, to, voxelShape.move(realPos).toAabbs());
								if (bl) {
									try {
										stepBasedCollector.advanceStep(-1);
										stepBasedCollector.advanceStep(0);
										blockState.entityInside(this.level, block.getPos(), self, stepBasedCollector);
										this.onInsideBlock(blockState);
									} catch (Exception ignored) {
									}
								}
								if (block.shouldDoFluidAction()) {
									AABB fluidBox = blockState.getFluidState().getAABB(this.level, block.getPos());
									if (fluidBox != null) {
										AABB zeroHitbox = fluidBox.move(BlockPos.ZERO.subtract(block.getPos()));
										if (this.collidedWithShapeMovingFrom(from, to, List.of(zeroHitbox.move(realPos)))) {
											try {
												stepBasedCollector.advanceStep(-1);
												stepBasedCollector.advanceStep(0);
												blockState.getFluidState().entityInside(this.level, block.getPos(), self, stepBasedCollector);
											} catch (Exception ignored) {
											}
										}
									}
								}
							}
						});
					}
				}
			}
			this.visitedBlocks.clear();
		}
	}
}
