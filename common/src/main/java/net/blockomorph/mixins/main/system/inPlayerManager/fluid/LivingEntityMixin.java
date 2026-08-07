package net.blockomorph.mixins.main.system.inPlayerManager.fluid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	private FluidState modifyState(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		var block = PlayersStorage.ofLevel(instance).findFirstBlockOnPos(this, this.getX(), this.getY(), this.getZ(), PlayersStorage.ALL_BLOCKS);
		if (block != null) {
			return block.getBlockState().getFluidState();
		}
		return original.call(instance, blockPos);
	}
}
