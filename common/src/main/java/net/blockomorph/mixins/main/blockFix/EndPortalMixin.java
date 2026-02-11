package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalMixin {

	@Shadow @Final protected static VoxelShape SHAPE;

	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	public void inside(BlockState blockState, Level level, BlockPos blockPos, Entity entity, CallbackInfo ci) {
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
			ci.cancel();
			if (level instanceof ServerLevel lv && entity.canChangeDimensions()) {
				List<AABB> aabbs = SHAPE.toAabbs();
				if (!aabbs.isEmpty()) {
					AABB aabb = aabbs.get(0);
					Vec3 vec = MorphUtils.getRealBlockPos(pl, realPos);
					if (aabb.move(vec).intersects(entity.getBoundingBox())) {
						ResourceKey<Level> resourceKey = level.dimension() == Level.END ? Level.OVERWORLD : Level.END;
						ServerLevel serverLevel = lv.getServer().getLevel(resourceKey);
						if (serverLevel == null) {
							return;
						}

						entity.changeDimension(serverLevel);
					}
				}
			}
		}, ci::cancel, level);
	}
}
