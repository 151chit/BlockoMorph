package net.blockomorph.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	private Level level;

	@Inject(method = "level", at = @At("HEAD"), cancellable = true)
	public void level(CallbackInfoReturnable<Level> cir) {
		if (this.level instanceof TntSpawnLevel lv) {
			this.level = lv.getRealLevel();
		}
	}
}
