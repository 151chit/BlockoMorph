package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.client.renderer.block.BlockModelLighter$Cache"})
public class ModelRendererMixin {

	@ModifyVariable(method = "getLightCoords", at = @At("HEAD"), require = 1)
	public BlockPos getReal(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@ModifyVariable(method = "getShadeBrightness", at = @At("HEAD"))
	public BlockPos getRealShade(BlockPos orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
	public void isBlockInGui(BlockState p_111222_, BlockAndTintGetter blockAndTintGetter, BlockPos p_111224_, CallbackInfoReturnable<Integer> cir) {
		if (blockAndTintGetter instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			cir.setReturnValue(LightCoordsUtil.pack(15, 15));
		}
	}
}
