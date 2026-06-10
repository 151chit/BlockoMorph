package net.blockomorph.utils.accessors;

import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface FluidTracker {
	MutableDouble fluidHeight$blockomorph();
	MutableBoolean eyeInside$blockomorph();
	void accumulateCurrent(Vec3 flow);

	static FluidTracker of(Object tr) {
		return (FluidTracker) tr;
	}
}
