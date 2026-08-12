package net.blockomorph.mixins.main.system.inPlayerManager.phys;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.phys.GapMovementCorrector;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Player.class, priority = 1020)
public abstract class AutoGapPlayerMixin implements PlayerAccessor {

	@FastInject(method = "maybeBackOffFromEdge", at = @At(value = "RETURN"))
	public Object fixMovement(Vec3 delta, MoverType moverType) {
		if (this.isBlockomorphActive() && (moverType == MoverType.PLAYER || moverType == MoverType.SELF)) {
			if (GapMovementCorrector.tryEnterInGap(this.player(), delta)) {
				return Vec3.ZERO;//suppress auto-step
			}
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}
