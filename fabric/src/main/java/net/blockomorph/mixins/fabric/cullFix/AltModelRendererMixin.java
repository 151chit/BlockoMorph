package net.blockomorph.mixins.fabric.cullFix;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderInfo.class)
public class AltModelRendererMixin {
	@Shadow public BlockAndTintGetter blockView;

	@Inject(method = "shouldDrawSide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private void getBlockStateInPlayer(Direction side, CallbackInfoReturnable<Boolean> cir) {
		if (this.blockView instanceof InPlayerBlockAndTintGetter playerProxy) {
			playerProxy.noExternal(true);
		}
	}

	@Inject(method = "shouldDrawSide", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private void getBlockStateInPlayerRelease(Direction side, CallbackInfoReturnable<Boolean> cir) {
		if (this.blockView instanceof InPlayerBlockAndTintGetter playerProxy) {
			playerProxy.noExternal(false);
		}
	}
}
