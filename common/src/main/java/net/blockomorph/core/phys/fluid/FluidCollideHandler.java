package net.blockomorph.core.phys.fluid;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public abstract class FluidCollideHandler<KEY> extends FluidWorker<KEY> {
	private final Object2ObjectMap<KEY, FluidCollideInfo> infosByKey = new Object2ObjectOpenHashMap<>();
	private final Entity owner;
	private Vec3 deltaMovement = Vec3.ZERO;

	public FluidCollideHandler(Entity self) {
		this.owner = self;
	}

	protected abstract double fluidScaleFactor(KEY key);
	protected abstract void setFluidHeight(KEY key, double height);
	protected void onFluidTouched(KEY key) {}

	@Override
	protected boolean isPushedByFluid(FluidState fluid) {
		return this.owner.isPushedByFluid();
	}

	@Override
	protected final FluidTracker trackerForBlock(FluidState fluid) {
		return this.infosByKey.computeIfAbsent(this.keyForBlock(fluid), FluidCollideInfo::new);
	}

	public void update(boolean doFluidPushing) {
		this.infosByKey.values().removeIf(FluidCollideInfo::resetAndCheckUnused);
		super.handleFluid(this.owner, this.owner.getBoundingBox().deflate(0.001), !doFluidPushing);
		this.deltaMovement = this.owner.getDeltaMovement();
	}

	public void applyAndRelease() {
		boolean needImpulse = this.owner.getDeltaMovement().subtract(this.deltaMovement).length() < Mth.EPSILON;
		for (FluidCollideInfo info : this.infosByKey.values()) {
			info.apply(needImpulse);
		}
		this.deltaMovement = Vec3.ZERO;
	}

	private class FluidCollideInfo implements FluidTracker {
		private final KEY key;
		private double fluidHeight;
		private int blockCount;
		private Vec3 flow = Vec3.ZERO;
		private int idleTicks;

		boolean resetAndCheckUnused() {
			this.fluidHeight = 0;
			this.blockCount = 0;
			this.flow = Vec3.ZERO;
			this.idleTicks++;
			return this.idleTicks > 20;
		}

		@Override
		public void onFluidTouched$bm() {
			this.idleTicks = 0;
			onFluidTouched(this.key);
		}

		private FluidCollideInfo(KEY key) {
			this.key = key;
		}

		@Override public void eyeInside$bm() {}

		@Override
		public void setFluidHeight$bm(double height) {
			this.fluidHeight = height;
		}

		@Override
		public double getFluidHeight$bm() {
			return this.fluidHeight;
		}

		@Override
		public void accumulateCurrent$bm(Vec3 vec) {
			this.flow = this.flow.add(vec);
			this.blockCount++;
		}

		void apply(boolean needImpulse) {
			if (this.blockCount != 0 && !(this.flow.lengthSqr() < Mth.EPSILON)) {
				Vec3 impulse;
				if (!(owner instanceof Player)) {
					impulse = this.flow.normalize();
				} else {
					impulse = this.flow.scale((double) 1.0F / (double) this.blockCount);
				}

				Vec3 oldMovement = owner.getDeltaMovement();
				impulse = impulse.scale(fluidScaleFactor(this.key));
				double min = 0.003;
				double scaleFactor = 0.0045;
				if (needImpulse && Math.abs(oldMovement.x) < min && Math.abs(oldMovement.z) < min && impulse.length() < scaleFactor) {
					impulse = impulse.normalize().scale(scaleFactor);
				}
				owner.addDeltaMovement(impulse);
			}
			if (this.fluidHeight > 0)
				setFluidHeight(this.key, this.fluidHeight);
		}
	}
}
