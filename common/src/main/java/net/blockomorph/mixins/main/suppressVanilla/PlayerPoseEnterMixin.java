package net.blockomorph.mixins.main.suppressVanilla;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerPoseEnterMixin implements PlayerAccessor {

	@FastInject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"))
	private byte suppress(Pose newPose) {
		if (this.isBlockomorphActive()) return 1;
		return 0;
	}
}
