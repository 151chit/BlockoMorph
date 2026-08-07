package net.blockomorph.mixins.main.system.inPlayerManager.chairController;

import net.blockomorph.core.misc.chairController.EntityChairController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Level.class)
public class LevelMixin {

	@Inject(method = "guardEntityTick", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
	private void checkChair(Consumer<Entity> tick, Entity entity, CallbackInfo ci) {
		if (entity instanceof EntityChairController.ChairPlayer ch) {
			ch.getHolder().forEachPassengers(passenger -> passenger.getController().rideTick(true));
		}
	}
}
