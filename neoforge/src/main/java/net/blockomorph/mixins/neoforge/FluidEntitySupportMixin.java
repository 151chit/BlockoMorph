package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FluidEntitySupportMixin implements IEntityExtension {
	@Shadow
	protected abstract void setFluidTypeHeight(FluidType type, double height);
	@Shadow private FluidType forgeFluidTypeOnEyes;

	@Inject(method = "updateFluidHeightAndDoFluidPushing(Z)V", at = @At(value = "HEAD"))
	public void handleFluid(boolean doFluidPushing, CallbackInfo ci) {
		var map = CommonPlatformUtils.handleFluidDetection((Entity) (Object)this,
				this::isPushedByFluid,
				fluidState -> !fluidState.getFluidType().isAir(),
				fluidState -> fluidState.getFluidType());
		CommonPlatformUtils.calculateLiquidsPower(doFluidPushing, (Entity) (Object)this, map,
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
