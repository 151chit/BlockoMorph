package net.blockomorph.mixins.main.system.morphState.deathPrepare;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public class LevelMixin {

	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
	public BlockState enterPreDeathState(BlockState original, BlockPos pos) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null) {
			if (pl.getManager().getFlags().isBreaking.isTrue()) {
				boolean isLiquid = original.getBlock() instanceof LiquidBlock;
				if (Config.get().playerDieAfterDestroy.getValue() && pl.size() == 1) {
					if (isLiquid || original.is(Blocks.AIR)) {
						return Blocks.VOID_AIR.defaultBlockState();
					}
				} else if (isLiquid) {
					return Blocks.AIR.defaultBlockState();
				}
			}
		}
		return original;
	}
}
