package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext")
public class BlockOcclusionCacheMixin {

	@Shadow protected BlockAndTintGetter level;

	@Inject(method = "shouldDrawSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.BEFORE))
	private void beforeGetBlockState(Direction facing, CallbackInfoReturnable<Boolean> cir) {
		LevelWithFlags.of(this.level).flags().morphedBlockGetterDisabled = true;
	}

	@Inject(method = "shouldDrawSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.AFTER))
	private void afterGetBlockState(Direction facing, CallbackInfoReturnable<Boolean> cir) {
		LevelWithFlags.of(this.level).flags().morphedBlockGetterDisabled = false;
	}
}
