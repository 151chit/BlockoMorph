package net.blockomorph.mixins.main.system.morphState.destroySupress;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public class LevelMixin {

	@FastInject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
	public byte rejectBreakCenterWhileMorph(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				if (pl.getManager().getFlags().morphProcess.isTrue() && blockState.is(Blocks.AIR) && posIn == InPlayerBlockPos.ZERO_INT)
					return -1;
			}
		}
		return 0;
	}

	@ModifyVariable(method = {"destroyBlock", "removeBlock"}, at = @At("STORE"))
	private FluidState preventSetFluidOnMorph(FluidState orig, BlockPos pos) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(pos.getX())) return Fluids.EMPTY.defaultFluidState();
		return orig;
	}
}
