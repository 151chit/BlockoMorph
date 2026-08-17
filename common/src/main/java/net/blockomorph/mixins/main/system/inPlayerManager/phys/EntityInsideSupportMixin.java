package net.blockomorph.mixins.main.system.inPlayerManager.phys;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.phys.EntityInsideCalculator;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityInsideSupportMixin {
	@Shadow protected abstract boolean isAffectedByBlocks();
	@Unique private final EntityInsideCalculator insideCalculator = new EntityInsideCalculator((Entity) (Object)this, InPlayerManager.HEAVY_MODE) {
		@Override
		protected void onBlockInside(BlockPos realOrKey, BlockState state) {
			if (currentCollector != null)
				currentCollector.add(state);
		}
	};
	@Unique private Set<BlockState> currentCollector;

	@FastInject(at = @At("HEAD"), method = "checkInsideBlocks")
	private boolean rewrite(List<?> list, Set<BlockState> set) {
		boolean vanilla = true;
		if (this.isAffectedByBlocks()) {
			vanilla = !this.insideCalculator.trySimplify();
			this.currentCollector = set;
			this.insideCalculator.calculateForPlayersCollide();
			this.currentCollector = null;
		}
		return vanilla;
	}
}
