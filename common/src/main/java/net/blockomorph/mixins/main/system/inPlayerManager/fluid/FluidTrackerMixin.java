package net.blockomorph.mixins.main.system.inPlayerManager.fluid;

import net.blockomorph.core.phys.fluid.FluidTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.entity.EntityFluidInteraction$Tracker")
public abstract class FluidTrackerMixin implements FluidTracker {
	@Shadow public abstract void accumulateCurrent(Vec3 flow);
	@Shadow private double height;
	@Shadow private boolean eyesInside;

	@Override public void eyeInside$bm() {
		this.eyesInside = true;
	}
	@Override public void setFluidHeight$bm(double height) {
		this.height = height;
	}
	@Override public double getFluidHeight$bm() {
		return this.height;
	}
	@Override public void accumulateCurrent$bm(Vec3 vec) {
		this.accumulateCurrent(vec);
	}
}
