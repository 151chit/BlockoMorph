package net.blockomorph.mixins.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IEntityExtension.class)
public interface ForgeEntityExtensionMixin {

	@Shadow
	private Entity self() { throw new IllegalStateException(); }

	@WrapOperation(method = "canStartSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState replaceLiquid(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		var block = PlayersStorage.ofLevel(instance).findFirstBlockOnPos(this.self(), this.self().getX(), this.self().getY(), this.self().getZ(), PlayersStorage.ALL_BLOCKS);
		if (block != null) {
			return block.getBlockState().getFluidState();
		}
		return original.call(instance, blockPos);
	}
}
