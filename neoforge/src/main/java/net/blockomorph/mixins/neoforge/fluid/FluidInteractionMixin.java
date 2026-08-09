package net.blockomorph.mixins.neoforge.fluid;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.blockomorph.core.phys.fluid.FluidCollideHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class FluidInteractionMixin implements IEntityExtension {
	@Shadow protected abstract void setFluidTypeHeight(FluidType par1, double par2);
	@Shadow protected Object2DoubleMap<FluidType> forgeFluidTypeHeight;
	@Unique private final FluidCollideHandler<FluidType> handler = new FluidCollideHandler<>((Entity) (Object)this) {
		@Override
		protected double fluidScaleFactor(FluidType key) {
			return getFluidMotionScale(key);
		}

		@Override
		protected FluidType keyForBlock(FluidState fluid) {
			return fluid.getType().getFluidType();
		}

		@Override
		protected boolean isPushedByFluid(FluidState fluid) {
			return FluidInteractionMixin.this.isPushedByFluid(this.keyForBlock(fluid));
		}

		@Override
		protected void setFluidHeight(FluidType fluidType, double height) {
			double old = forgeFluidTypeHeight.getOrDefault(fluidType, 0);
			if (old < height) FluidInteractionMixin.this.setFluidTypeHeight(fluidType, height);
		}
	};

	@WrapMethod(method = "updateFluidHeightAndDoFluidPushing(Z)V")
	private void calculate(boolean doFluidPushing, Operation<Void> original) {
		this.handler.update(doFluidPushing);
		original.call(doFluidPushing);
		this.handler.applyAndRelease();
	}
}
