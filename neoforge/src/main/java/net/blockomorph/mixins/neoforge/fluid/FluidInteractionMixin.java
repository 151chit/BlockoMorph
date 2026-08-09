package net.blockomorph.mixins.neoforge.fluid;

import net.blockomorph.core.phys.fluid.FluidTracker;
import net.blockomorph.core.phys.fluid.FluidWorker;
import net.minecraft.tags.TagKey;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
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
	@Shadow @Final private Map<FluidType, FluidTracker> trackerByFluid;
	@Unique
	private final FluidWorker<?> worker = new FluidWorker<>() {
		@Override
		protected Object keyForBlock(FluidState fluid) {
			if (vanillaKeys.isDefault()) getTracker(fluid);
			if (vanillaKeys.isTrue())
				return fluid.getType();
			return fluid.getType().getFluidType();
		}

		@Override
		protected boolean isPushedByFluid(FluidState fluid) {
			return currentEntity.isPushedByFluid(fluid.getFluidType());
		}

		@Override
		protected FluidTracker trackerForBlock(FluidState fluid) {
			return getTracker(fluid);
		}
	};
	@Unique private Entity currentEntity;

	@Inject(method = "update", at = @At("TAIL"))
	private void handle(Entity entity, boolean ignoreCurrent, CallbackInfo ci) {
		this.currentEntity = entity;
		this.worker.handleFluid(entity, entity.getFluidInteractionBox(), ignoreCurrent);
		this.currentEntity = null;
	}




	@Unique TriState vanillaKeys = TriState.DEFAULT;
	//TODO: remove after https://github.com/neoforged/NeoForge/pull/3249/changes
	//https://github.com/neoforged/NeoForge/pull/3303
	@Unique
	private FluidTracker getTracker(FluidState fluid) {
		if (vanillaKeys.isFalse()) {
			return trackerByFluid.computeIfAbsent(fluid.getType().getFluidType(), _ ->
					FluidTracker.of(new EntityFluidInteraction.Tracker()));

		} else if (vanillaKeys.isTrue()) {
			return byTag(fluid);
		}


		if (this.trackerByFluid.isEmpty()) {
			vanillaKeys = TriState.FALSE;
			return getTracker(fluid);
		}
		for (Object key : this.trackerByFluid.keySet()) {
			if (key instanceof FluidType) {
				vanillaKeys = TriState.FALSE;
				return getTracker(fluid);
			} else break;
		}
		vanillaKeys = TriState.TRUE;
		return getTracker(fluid);
	}

	@Unique
	private FluidTracker byTag(FluidState fluid) {
		for(Map.Entry<?, FluidTracker> entry : trackerByFluid.entrySet()) {
			Object tag = entry.getKey();
			if (tag instanceof TagKey<?> tag2 && fluid.is((TagKey<Fluid>) tag2)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
