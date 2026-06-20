package net.blockomorph.mixins.main.block.blockUpdates;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.accessors.BlockSignalGetter;
import net.blockomorph.utils.accessors.PlayersProvider;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings("MissingUnique")
@Mixin(SignalGetter.class)
public interface SignalGetterMixin {
	@Shadow int getDirectSignal(BlockPos p_277954_, Direction p_277342_);

	@ModifyReturnValue(method = "getSignal", at = @At("RETURN"))
	private int checkSg(int original, BlockPos blockPos, Direction direction) {
		return this.checkSignal$blockomorph$internal(original, blockPos, direction, BlockBehaviour.BlockStateBase::getSignal);
	}

	@ModifyReturnValue(method = "getDirectSignal", at = @At("RETURN"))
	private int checkDirSg(int original, BlockPos blockPos, Direction direction) {
		return this.checkSignal$blockomorph$internal(original, blockPos, direction, BlockBehaviour.BlockStateBase::getDirectSignal);
	}

	@ModifyVariable(method = "getControlInputSignal", at = @At("STORE"))
	private BlockState checkDiodes(BlockState orig, BlockPos pos, Direction dir, boolean diodeNeed) {
		if (Config.get().dynamicRedstone.getValue() && this instanceof PlayersProvider pr) {
			if (diodeNeed && DiodeBlock.isDiode(orig)) return orig;

			var playerBlocks = pr.getStorage$blockomorph().getPlayersOnPos(pos.asLong());
			if (!playerBlocks.isEmpty()) {
				BlockState power = this.setOutputPower$blockomorph$internal(this.getDirectSignal(pos, dir));
				for (Object block : playerBlocks.getIterateArray()) {
					if (!(block instanceof BlockInPlayer2 blockInPl)) continue;
					if (!EntitySelector.NO_SPECTATORS.test(blockInPl.getPlayer().player())) continue;
					BlockState blockState = blockInPl.getBlockState();
					if (diodeNeed) {
						if (DiodeBlock.isDiode(blockState)) {
							return blockState;
						} else continue;
					}
					if (blockState.is(Blocks.REDSTONE_BLOCK)) return blockState;
					int currPower = blockState.is(Blocks.REDSTONE_WIRE) ? blockState.getValue(RedStoneWireBlock.POWER) : this.getDirectSignal(blockInPl.getPos(), dir);
					if (currPower > power.getValue(RedStoneWireBlock.POWER)) {
						power = this.setOutputPower$blockomorph$internal(currPower);
					}
				}
				if (!diodeNeed) return power;
			}
		}
		return orig;
	}

	default BlockState setOutputPower$blockomorph$internal(int i) {
		return Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.POWER, i);
	}

	default int checkSignal$blockomorph$internal(int orig, BlockPos origPos, Direction direction, BlockSignalGetter sgGet) {
		if (Config.get().dynamicRedstone.getValue() && this instanceof PlayersProvider pr && orig < 15) {
			var playerBlocks = pr.getStorage$blockomorph().getPlayersOnPos(InPlayerBlockPos.checkOnReal(origPos).asLong());
			if (!playerBlocks.isEmpty()) {
				int max = orig;
				for (Object block : playerBlocks.getIterateArray()) {
					if (!(block instanceof BlockInPlayer2 blockInPl)) continue;
					if (!EntitySelector.NO_SPECTATORS.test(blockInPl.getPlayer().player())) continue;
					max = Math.max(max, sgGet.getSignal(blockInPl.getBlockState(), (BlockGetter) this, blockInPl.getPos(), direction));
					if (max > 14) return max;
				}
				return max;
			}
		}
		return orig;
	}
}
