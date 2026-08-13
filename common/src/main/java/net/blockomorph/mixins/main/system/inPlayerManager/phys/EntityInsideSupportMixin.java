package net.blockomorph.mixins.main.system.inPlayerManager.phys;

import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.phys.EntityInsideCalculator;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityInsideSupportMixin {
	@Shadow protected abstract boolean isAffectedByBlocks();
	@Unique private final EntityInsideCalculator insideCalculator = new EntityInsideCalculator((Entity) (Object)this, InPlayerManager.HEAVY_MODE);

	@FastInject(at = @At("HEAD"), method = "checkInsideBlocks(Ljava/util/List;Lnet/minecraft/world/entity/InsideBlockEffectApplier$StepBasedCollector;)V")
	private boolean rewrite(List<?> movements, InsideBlockEffectApplier.StepBasedCollector effectCollector) {
		boolean vanilla = true;
		if (this.isAffectedByBlocks()) {
			vanilla = !this.insideCalculator.trySimplify();
			this.insideCalculator.calculateForPlayersCollide();
		}
		return vanilla;
	}
}
