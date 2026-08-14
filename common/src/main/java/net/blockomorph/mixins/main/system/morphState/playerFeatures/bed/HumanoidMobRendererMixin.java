package net.blockomorph.mixins.main.system.morphState.playerFeatures.bed;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidMobRenderer.class, priority = 1001)
public class HumanoidMobRendererMixin {

	@Inject(at = @At("TAIL"), method = "extractHumanoidRenderState")
	private static void prepareForBed(LivingEntity entity, HumanoidRenderState state, float partialTicks, ItemModelResolver itemModelResolver, CallbackInfo ci) {
		if (entity.getSleepingPos().isPresent()) {
			BlockPos sleepingPos = entity.getSleepingPos().get();
			if (InPlayerBlockPos.isMorphedPlayerBlockX(sleepingPos.getX())) {
				state.isPassenger = false;
				state.yRot = 0;
			}
		}
	}
}
