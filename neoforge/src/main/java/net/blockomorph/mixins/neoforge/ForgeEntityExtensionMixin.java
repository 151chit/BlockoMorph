package net.blockomorph.mixins.neoforge;

import net.neoforged.neoforge.common.extensions.IEntityExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IEntityExtension.class)
public interface ForgeEntityExtensionMixin {

	/*@WrapOperation(method = "canStartSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState replaceLiquid(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		Entity entity = (Entity) this;
		var entry = MorphUtils.getLiquidOnPos(entity, entity.position());
		if (entry != null) {
			return entry.isTrue().getBlockState().getFluidState();
		}
		return original.call(instance, blockPos);
	}*/// <- rewrite fluids?
}
