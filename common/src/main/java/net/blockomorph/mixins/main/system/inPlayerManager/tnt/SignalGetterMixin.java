package net.blockomorph.mixins.main.system.inPlayerManager.tnt;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.utils.side.Side;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.mixin.FastInject;
import net.blockomorph.utils.mixin.PrimitiveCancelSignal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignalGetter.class)
public interface SignalGetterMixin {

	@FastInject(method = "getSignal", at = @At("HEAD"), continueIf = @PrimitiveCancelSignal(intValue = -1))
	private int checkFlag(BlockPos pos, Direction direction) {
		if (this instanceof LevelWithFlags acc && acc.flags().redstoneAlwaysOn && Side.get() == Side.SERVER) {
			return 15;
		}
		return -1;
	}

	@ModifyReturnValue(method = "getDirectSignal", at = @At("RETURN"))
	private int checkFlag(int original) {
		if (this instanceof LevelWithFlags acc && acc.flags().redstoneAlwaysOn && Side.get() == Side.SERVER) {
			return 15;
		}
		return original;
	}
}
