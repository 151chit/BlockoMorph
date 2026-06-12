package net.blockomorph.mixins.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IEntityExtension.class)
public interface ForgeEntityExtensionMixin {

	@WrapOperation(method = "canStartSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState replaceLiquid(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		Entity entity = (Entity) this;
		var blocks = PlayersMultiSectionStorage.getBlockOnPos(entity, entity.level(), entity.position());
		if (!blocks.isEmpty()) {
			return blocks.iterator().next().getBlockState().getFluidState();
		}
		return original.call(instance, blockPos);
	}
}
