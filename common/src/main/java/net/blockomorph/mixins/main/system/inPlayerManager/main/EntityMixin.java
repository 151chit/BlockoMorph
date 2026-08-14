package net.blockomorph.mixins.main.system.inPlayerManager.main;

import net.blockomorph.core.PlayerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class EntityMixin {

	@ModifyVariable(method = "setLevel", at = @At("HEAD"))
	private Level changeLv(Level orig) {
		if (this instanceof PlayerAccessor pl && !pl.isNotInitialized())
			pl.getManager().onLevelChange(orig);
		return orig;
	}
}
