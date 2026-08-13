package net.blockomorph.mixins.fabric.fluid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class SwimmingEntityMixin {
	@Shadow public abstract double getX();
	@Shadow public abstract double getY();
	@Shadow public abstract double getZ();

	@WrapOperation(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	public FluidState getRealBlock(Level instance, BlockPos pos, Operation<FluidState> original) {
		FluidState fluidState = original.call(instance, pos);
		if (!fluidState.is(FluidTags.WATER)) {
			var block = PlayersStorage.ofLevel(instance).findFirstBlockOnPos(this.getThis(), this.getX(), this.getY(), this.getZ(), PlayersStorage.ALL_BLOCKS);
			if (block != null) {
				return block.getBlockState().getFluidState();
			}
		}
		return fluidState;
	}

	@Unique
	private Entity getThis() {
		return (Entity)(Object) this;
	}
}
