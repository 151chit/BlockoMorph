package net.blockomorph.mixins.main;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityInsideSupportMixin {

	@Shadow public abstract AABB getBoundingBox();

	@Shadow private Level level;

	@Shadow protected abstract void onInsideBlock(BlockState blockState);

	@Shadow @Final private LongSet visitedBlocks;

	@Shadow
	protected abstract AABB makeBoundingBox(Vec3 vec3);

	@Shadow
	public abstract boolean isAlive();

	@Inject(method = "checkInsideBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;boxTraverseBlocks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;)Ljava/lang/Iterable;"))
	private void checkInsideBlocks(List<?> list, Set<BlockState> set, CallbackInfo ci, @Local(ordinal = 0) Vec3 from, @Local(ordinal = 1) Vec3 to, @Local AABB aABB) {
		if (Config.get().entityInside.getValue()) {
			Entity self = (Entity) (Object) this;
			for (PlayerAccessor pl : PlayersMultiSectionStorage.fromLevel(this.level).findMorphed(self, aABB)) {
				pl.getBlocksData2InArea(aABB, (pos, block, realPos) -> {
					if (!this.isAlive()) return;
					BlockState blockState = block.getBlockState();
					if (!blockState.isAir() && this.visitedBlocks.add(block.getPos().asLong())) {
						VoxelShape voxelShape = blockState.getEntityInsideCollisionShape(this.level, block.getPos());
						if (voxelShape != Shapes.block() && !this.collide(from, to, MorphUtils.getRealBlockPos(block.getPlayer(), block.getOffset()), voxelShape)) {
							return;
						}
						blockState.entityInside(this.level, block.getPos(), self);
						this.onInsideBlock(blockState);
						set.add(blockState);
					}
				});
			}
		}
	}

	@Unique
	private boolean collide(Vec3 from, Vec3 to, Vec3 blockPos, VoxelShape voxelShape) {
		AABB aABB = this.makeBoundingBox(from);
		Vec3 vec33 = to.subtract(from);
		return aABB.collidedAlongVector(vec33, voxelShape.move(blockPos).toAabbs());
	}
}
