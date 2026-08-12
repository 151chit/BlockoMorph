package net.blockomorph.mixins.main.system.inPlayerManager.loginAndSerialize.freeze;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class PlayerMixin {

	@FastInject(method = "move", at = @At("HEAD"))
	private boolean freeze(MoverType moverType, Vec3 delta) {
		return !(this instanceof PlayerAccessor pl) || pl.getManager().isNotInitialized() ||
				!pl.getManager().getFlags().serializingProcess.isTrue();
	}
}
