package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(Entity.class)
public abstract class FluidEntitySupportMixin implements IForgeEntity {
	@Shadow(remap = false)
	protected abstract void setFluidTypeHeight(FluidType type, double height);
	@Shadow(remap = false) private FluidType forgeFluidTypeOnEyes;

	@Inject(method = "updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V", at = @At(value = "TAIL"), remap = false)
	public void handleFluid(Predicate<FluidState> shouldUpdate, CallbackInfo ci) {
		var map = CommonPlatformUtils.handleFluidDetection((Entity) (Object)this,
				shouldUpdate::test,
				fluidState -> !fluidState.getFluidType().isAir(),
				fluidState -> fluidState.getFluidType());
		CommonPlatformUtils.calculateLiquidsPower((Entity) (Object)this, map,
				this::getFluidMotionScale,
				this::setFluidTypeHeight);
	}

	@Inject(method = "updateFluidOnEyes", at = @At("TAIL"))
	public void getLiquid(CallbackInfo ci) {
		CommonPlatformUtils.handleUpdateFluidOnEyes((Entity) (Object)this, fluidState -> {
			this.forgeFluidTypeOnEyes = fluidState.getFluidType();
		});
	}
}
