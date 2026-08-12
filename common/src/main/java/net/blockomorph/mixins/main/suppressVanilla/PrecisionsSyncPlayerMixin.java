package net.blockomorph.mixins.main.suppressVanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerEntity.class)
public class PrecisionsSyncPlayerMixin {
	@Shadow private boolean wasOnGround;

	@WrapOperation(method = "sendChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onGround()Z"))
	private boolean suppressInterpolation(Entity instance, Operation<Boolean> original) {
		boolean origRevert = !this.wasOnGround;
		if (instance instanceof Player) return origRevert;
		return original.call(instance);
	}
}
