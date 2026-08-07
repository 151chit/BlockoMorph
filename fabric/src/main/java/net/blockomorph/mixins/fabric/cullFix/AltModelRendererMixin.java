package net.blockomorph.mixins.fabric.cullFix;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(AltModelBlockRendererImpl.class)
public class AltModelRendererMixin {
	@Shadow private BlockAndTintGetter level;
	@Shadow @Final private BlockPos.MutableBlockPos scratchPos;
	@Shadow private BlockPos pos;

	@ModifyVariable(method = "shouldCullFace", at = @At(value = "STORE"), ordinal = 0)
	private BlockState getBlockStateInPlayer(BlockState neighborState, Direction direction) {
		if (this.level instanceof InPlayerBlockAndTintGetter playerProxy) {
			if (playerProxy.isExternalPos(this.scratchPos.setWithOffset(this.pos, direction)))
				return Blocks.AIR.defaultBlockState();
		}
		return neighborState;
	}
}
