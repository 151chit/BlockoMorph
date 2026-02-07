package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.accessors.LevelAcc;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(Level.class)
public class LevelInjectsMixin {

	@ModifyVariable(method = "markAndNotifyBlock", at = @At(value = "STORE"), ordinal = 2)
	public BlockState getState(BlockState value, BlockPos pos) {
		AtomicReference<BlockState> state = new AtomicReference<>(value);
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			state.set(pl.getBlockState(realPos));
		}, null, LevelAcc.of(this));
		return state.get();
	}
}
