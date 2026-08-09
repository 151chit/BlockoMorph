package net.blockomorph.mixins.fabric.fluid;

import net.blockomorph.core.phys.fluid.FluidTracker;
import net.blockomorph.core.phys.fluid.FluidWorker;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(EntityFluidInteraction.class)
public abstract class FluidInteractionMixin {
	@Shadow @Final private Map<TagKey<Fluid>, FluidTracker> trackerByFluid;
	@Unique private final FluidWorker<Fluid> worker = new FluidWorker<>() {
		@Override
		protected Fluid keyForBlock(FluidState fluid) {
			Fluid fluidType = fluid.getType();
			if (fluidType instanceof FlowingFluid flowingFluid) {
				fluidType = flowingFluid.getSource();
			}
			return fluidType;
		}

		@Override
		protected FluidTracker trackerForBlock(FluidState fluid) {
			for (Map.Entry<TagKey<Fluid>, FluidTracker> entry : trackerByFluid.entrySet()) {
				TagKey<Fluid> tag = entry.getKey();
				if (fluid.is(tag)) {
					return entry.getValue();
				}
			}
			return null;
		}
	};

	@Inject(method = "update", at = @At("TAIL"))
	private void handle(Entity entity, boolean ignoreCurrent, CallbackInfo ci) {
		this.worker.handleFluid(entity, entity.getFluidInteractionBox(), ignoreCurrent);
	}
}
