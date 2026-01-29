package net.blockomorph.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@WrapOperation(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	public BlockState run(BlockAndTintGetter instance, BlockPos blockPos, Operation<BlockState> original) {
		AtomicReference<BlockState> state = new AtomicReference<>(original.call(instance, blockPos));
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> state.set(pl.getBlockState(realPos)), null, true);
		return state.get();
	}

	@ModifyVariable(ordinal = 3, method = {
			"tesselateWithoutAO",
	}, at = @At("STORE"))
	private int changeLight(int i, BlockAndTintGetter level) {
		if (level instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			return LightTexture.pack(15, 15);
		}
		return i;
	}
}
