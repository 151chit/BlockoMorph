package net.blockomorph.mixins.fabric.fluid;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.blockomorph.core.phys.fluid.FluidCollideHandler;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class FluidInteractionMixin {
	@Shadow protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;
	@Unique private final FluidCollideHandler<Fluid> handler = new FluidCollideHandler<>((Entity) (Object)this) {//still fluid

		@Override
		protected Fluid keyForBlock(FluidState fluid) {
			if (!fluid.is(thisTickKey)) return null;
			Fluid fluidType = fluid.getType();
			if (fluidType instanceof FlowingFluid flowing) {
				fluidType = flowing.getSource();
			}
			return fluidType;
		}

		@Override
		protected void onFluidTouched(Fluid fluid) {
			fluidTouched = true;
		}

		@Override
		protected double fluidScaleFactor(Fluid fluid) {
			return thisTickFlowScale;
		}

		@Override
		protected void setFluidHeight(Fluid fluid, double height) {
			double old = fluidHeight.getOrDefault(thisTickKey, 0);
			if (old < height) {
				fluidHeight.put(thisTickKey, height);
			}
		}
	};
	@Unique private TagKey<Fluid> thisTickKey;
	@Unique private double thisTickFlowScale;
	@Unique private boolean fluidTouched;

	@WrapMethod(method = "updateFluidHeightAndDoFluidPushing")
	private boolean calculate(TagKey<Fluid> tagKey, double flowScale, Operation<Boolean> original) {
		this.thisTickKey = tagKey;
		this.thisTickFlowScale = flowScale;
		this.fluidTouched = false;

		this.handler.update(true);
		boolean vanillaYes = original.call(tagKey, flowScale);
		this.handler.applyAndRelease();
		return this.fluidTouched || vanillaYes;
	}
}
