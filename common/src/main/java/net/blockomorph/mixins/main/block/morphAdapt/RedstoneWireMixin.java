package net.blockomorph.mixins.main.block.morphAdapt;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.accessors.PlayersProvider;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireMixin {

	@Shadow
	protected abstract int getWireSignal(BlockState p_55649_);

	@ModifyVariable(method = "calculateTargetStrength", at = @At(value = "STORE", ordinal = 1), ordinal = 1)
	private int direct(int origPower, Level lv, @Local(ordinal = 1) BlockPos blockPos2) {
		return Math.max(origPower, this.findMaxPlayerPower(lv, InPlayerBlockPos.checkOnReal(blockPos2).asLong()));
	}

	@ModifyVariable(method = "calculateTargetStrength", at = @At(value = "STORE", ordinal = 2), ordinal = 1)
	private int up(int origPower, Level lv, @Local(ordinal = 1)  BlockPos blockPos2) {
		BlockPos blockPos = InPlayerBlockPos.checkOnReal(blockPos2);
		return Math.max(origPower, this.findMaxPlayerPower(lv, BlockPos.asLong(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ())));
	}

	@ModifyVariable(method = "calculateTargetStrength", at = @At(value = "STORE", ordinal = 3), ordinal = 1)
	private int down(int origPower, Level lv, @Local(ordinal = 1) BlockPos blockPos2) {
		BlockPos blockPos = InPlayerBlockPos.checkOnReal(blockPos2);
		return Math.max(origPower, this.findMaxPlayerPower(lv, BlockPos.asLong(blockPos.getX(), blockPos.getY() - 1, blockPos.getZ())));
	}

	@Unique
	private int findMaxPlayerPower(Level lv, long blockPos) {
		if (Config.get().dynamicRedstone.getValue()) {
			var playerBlocks = PlayersProvider.of(lv).getStorage$blockomorph().getPlayersOnPos(blockPos);
			if (!playerBlocks.isEmpty()) {
				int max = 0;
				for (Object block : playerBlocks.getIterateArray()) {
					if (!(block instanceof BlockInPlayer2 blockInPl)) continue;
					if (!EntitySelector.NO_SPECTATORS.test(blockInPl.getPlayer().player())) continue;
					if (blockInPl.getBlockState().getBlock() instanceof RedStoneWireBlock) {
						max = Math.max(max, this.getWireSignal(blockInPl.getBlockState()));
					}
				}
				return max;
			}
		}
		return 0;
	}
}
