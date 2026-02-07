package net.blockomorph.mixins.main.client.graphic.entity;

import net.blockomorph.utils.accessors.AvatarRenderStateAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At(value = "HEAD"))
	public void fixRiding(T humanoidRenderState, CallbackInfo ci) {
		this.fix(humanoidRenderState, () -> humanoidRenderState.isPassenger = false);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At(value = "TAIL"))
	public void fixRiding2(T humanoidRenderState, CallbackInfo ci) {
		this.fix(humanoidRenderState, () -> ((HumanoidModel<?>) (Object) this).head.yRot = 0);
	}

	@Unique
	private void fix(HumanoidRenderState livingEntity, Runnable cons) {
		if (livingEntity instanceof AvatarRenderStateAccessor acc && acc.getMorphedRenderStateStorage().get() != null) {
			BlockPos sleepingPos = acc.getMorphedRenderStateStorage().getOpaque().sleepingPos;
			if (sleepingPos != null) {
				if (InPlayerBlockPos.isMorphedPlayerX(sleepingPos.getX())) {
					cons.run();
				}
			}
		}
	}

}
