package net.blockomorph.mixins.main.system.inPlayerManager.phys.hitbox;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class AvatarMixin {

	@FastInject(method = "getDefaultDimensions", at = @At("HEAD"))
	public Object getDimensionsDefault(Pose pose) {
		if (this instanceof PlayerAccessor acc && acc.isBlockomorphActive()) {
			return acc.getManager().getHitBoxCalculator().getDimensions();
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}
