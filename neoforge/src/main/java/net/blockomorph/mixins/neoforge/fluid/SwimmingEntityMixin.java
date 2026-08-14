package net.blockomorph.mixins.neoforge.fluid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IEntityExtension.class)
public interface SwimmingEntityMixin {

	@WrapOperation(method = "canStartSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	default FluidState getRealBlock(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		FluidState fluidState = original.call(instance, blockPos);
		if (!fluidState.is(FluidTags.WATER)) {
			var block = PlayersStorage.ofLevel(instance).findFirstBlockOnPos(this.getThis(),
					this.getThis().getX(), this.getThis().getY(), this.getThis().getZ(), PlayersStorage.ALL_BLOCKS);
			if (block != null) {
				return block.getBlockState().getFluidState();
			}
		}
		return fluidState;
	}

	@Unique
	private Entity getThis() {
		return (Entity) this;
	}
}
