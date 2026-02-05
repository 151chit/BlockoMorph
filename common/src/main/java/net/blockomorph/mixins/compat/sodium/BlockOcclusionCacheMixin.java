package net.blockomorph.mixins.compat.sodium;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache")
public class BlockOcclusionCacheMixin {
	@Shadow @Final private BlockPos.MutableBlockPos cachedPositionObject;

	@ModifyVariable(method = "shouldDrawSide", at = @At(value = "STORE"), ordinal = 1)
	private BlockState getRealState(BlockState value, BlockState selfState, BlockGetter view, BlockPos selfPos, Direction facing) {
		if (InPlayerBlockPos.isMorphedPlayerX(selfPos.getX())) {
			AtomicReference<BlockState> input = new AtomicReference<>(value);
			InPlayerBlockPos.check(this.cachedPositionObject, (pl, realPos) -> {
				input.set(pl.getBlockState(realPos));
			}, null, true);
			return input.get();
		}
		return value;
	}
}
