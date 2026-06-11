package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext")
public class BlockOcclusionCacheMixin {

	@Shadow protected BlockAndTintGetter level;

	@Inject(method = "shouldDrawSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.BEFORE))
	private void beforeGetBlockState(Direction facing, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		ClientLevelAccessor.of(this.level).lockExternalMorphedBlockGetter(true);
	}

	@Inject(method = "shouldDrawSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.AFTER))
	private void afterGetBlockState(Direction facing, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		ClientLevelAccessor.of(this.level).lockExternalMorphedBlockGetter(false);
	}
}
