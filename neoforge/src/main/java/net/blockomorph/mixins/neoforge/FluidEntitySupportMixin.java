package net.blockomorph.mixins.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class FluidEntitySupportMixin implements IEntityExtension {
	@Shadow private Vec3 position;

	@Shadow
	public abstract Level level();

	@WrapOperation(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	public FluidState getRealBlock(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		FluidState fluidState = original.call(instance, blockPos);
		if (!fluidState.is(FluidTags.WATER)) {
			var blocks = PlayersMultiSectionStorage.getBlockOnPos((Entity) (Object) this, this.level(), this.position);
			if (!blocks.isEmpty()) return blocks.iterator().next().getBlockState().getFluidState();
		}
		return fluidState;
	}//TODO: <- FORGE ENTITY EXTENSION
}
