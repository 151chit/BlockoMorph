package net.blockomorph.mixins.main;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityInsideSupportMixin {

	@Shadow public abstract AABB getBoundingBox();

	@Shadow private Level level;

	@Shadow protected abstract void onInsideBlock(BlockState blockState);

	@Inject(at = @At(value = "TAIL"), method = "checkInsideBlocks")
	private void checkInsideBlocks(CallbackInfo ci) {
		if (Config.get().entityInside.getValue()) {
			AABB entityBox = this.getBoundingBox().deflate(1.0E-5F);
			Entity self = (Entity) (Object) this;
			List<Entity> entities = this.level.getEntities(self, entityBox, EntitySelector.NO_SPECTATORS);
			for (Entity entity : entities) {
				if (entity instanceof PlayerAccessor pl && pl.isFullActive()) {
					pl.getBlocksData2InArea(entityBox, (pos, block, realPos) -> {
						BlockState blockState = block.getBlockState();
						if (!blockState.isAir()) {
							blockState.entityInside(this.level, block.getPos(), self);
							this.onInsideBlock(blockState);
						}
					});
				}
			}
		}
	}
}
