package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@ModifyVariable(ordinal = 3, method = {
			"tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/function/Function;ZI)V",
	}, at = @At("STORE"))
	private int changeLight(int i, BlockAndTintGetter level) {
		if (level instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			return LightTexture.pack(15, 15);
		}
		return i;
	}

	@ModifyVariable(method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z", at = @At(value = "STORE"), ordinal = 1)
	private static BlockState getBlockStateInPlayer(BlockState original, BlockAndTintGetter p_412640_, BlockPos pos, BlockState p_412168_, boolean p_412054_, Direction p_412130_, BlockPos blockPos) {
		AtomicReference<BlockState> state = new AtomicReference<>(original);
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> state.set(pl.getBlockState(realPos)), null, true);
		return state.get();
	}
}
