package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.client.renderer.block.ModelBlockRenderer$Cache"})
public class ModelRendererMixin {

	@ModifyVariable(method = "getLightColor", at = @At("HEAD"), require = 1)
	public BlockPos getReal(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@ModifyVariable(method = "getShadeBrightness", at = @At("HEAD"))
	public BlockPos getRealShade(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true)
	public void isBlockInGui(BlockState p_111222_, BlockAndTintGetter blockAndTintGetter, BlockPos p_111224_, CallbackInfoReturnable<Integer> cir) {
		if (blockAndTintGetter instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			cir.setReturnValue(LightTexture.pack(15, 15));
		}
	}
}
