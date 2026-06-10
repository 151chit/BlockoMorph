package net.blockomorph.mixins.main;

import net.blockomorph.utils.accessors.FluidTracker;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.world.entity.EntityFluidInteraction$Tracker")
public abstract class FluidTrackerMixin implements FluidTracker {
	@Shadow private double height;
	@Shadow private boolean eyesInside;
	@Unique private final MutableDouble fluidHeight = new MutableDouble() {
		@Override
		public void setValue(double value) {
			height = value;
		}

		@Override
		public double doubleValue() {
			return height;
		}
	};
	@Unique private final MutableBoolean eyeInside = new MutableBoolean() {
		@Override
		public void setValue(boolean value) {
			eyesInside = value;
		}

		@Override
		public boolean booleanValue() {
			return eyesInside;
		}
	};

	@Override
	public MutableDouble fluidHeight$blockomorph() {
		return this.fluidHeight;
	}

	@Override
	public MutableBoolean eyeInside$blockomorph() {
		return this.eyeInside;
	}
}
