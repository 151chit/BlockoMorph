package net.blockomorph.mixins.main.system.inPlayerManager.chairController;

import net.blockomorph.core.misc.chairController.EntityChairController;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidMobRenderer.class)
public class HumanoidMobRendererMixin {

	@Inject(method = "extractHumanoidRenderState",
			at = @At(value = "TAIL"))
	private static void activateChairAnim(LivingEntity entity, HumanoidRenderState state, float partialTicks, ItemModelResolver itemModelResolver, CallbackInfo ci) {
		if (entity instanceof EntityChairController.ChairedEntity ch && ch.getController().isActive())
			state.isPassenger = true;
	}
}
