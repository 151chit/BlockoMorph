package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.Portal;
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
public abstract class EndPortalMixin implements Portal {

	@Shadow @Final protected static VoxelShape SHAPE;

	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	public void inside(BlockState blockState, Level level, BlockPos blockPos, Entity entity, CallbackInfo ci) {
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
			ci.cancel();
			if (entity.canUsePortal(false)) {
				List<AABB> aabbs = SHAPE.toAabbs();
				if (!aabbs.isEmpty()) {
					AABB aabb = aabbs.getFirst();
					Vec3 vec = MorphUtils.getRealBlockPos(pl, realPos);
					if (aabb.move(vec).intersects(entity.getBoundingBox())) {
						if (!level.isClientSide && level.dimension() == Level.END && entity instanceof ServerPlayer serverPlayer) {
							if (!serverPlayer.seenCredits) {
								serverPlayer.showEndCredits();
								return;
							}
						}

						entity.setAsInsidePortal(this, blockPos);
					}
				}
			}
		}, ci::cancel, level);
	}
}
