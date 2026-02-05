package net.blockomorph.mixins.main.client.graphic.entity;

import net.blockomorph.utils.accessors.RenderStateAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

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
		if (livingEntity instanceof RenderStateAccessor acc) {
			Optional<BlockPos> pos = acc.getPlayer().getSleepingPos();
			pos.ifPresent((blockpos) -> {
				if (InPlayerBlockPos.isMorphedPlayerX(blockpos.getX())) {
					cons.run();
				}
			});
		}
	}

}
