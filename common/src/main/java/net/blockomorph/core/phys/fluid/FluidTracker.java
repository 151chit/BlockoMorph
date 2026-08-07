package net.blockomorph.core.phys.fluid;

import net.minecraft.world.phys.Vec3;

public interface FluidTracker {
	void eyeInside$bm();
	void setFluidHeight$bm(double height);
	double getFluidHeight$bm();
	void accumulateCurrent$bm(Vec3 vec);
	default void doAdditional$bm() {}

	static FluidTracker of(Object tr) {
		return (FluidTracker) tr;
	}
}
