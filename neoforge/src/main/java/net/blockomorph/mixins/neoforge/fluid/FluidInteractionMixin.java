package net.blockomorph.mixins.neoforge.fluid;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.phys.fluid.FluidTracker;
import net.blockomorph.core.phys.fluid.FluidWorker;
import net.minecraft.tags.TagKey;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
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
	private final FluidWorker worker = new FluidWorker() {
		@Override
		protected Object keyForBlock(BlockInPlayer2 block) {
			if (vanillaKeys.isDefault()) getTracker(block);
			if (vanillaKeys.isTrue())
				return this.blockToFluidState(block).getType();
			return this.blockToFluidState(block).getType().getFluidType();
		}

		@Override
		protected FluidTracker trackerForBlock(BlockInPlayer2 block) {
			return getTracker(block);
		}
	};

	@Inject(method = "update", at = @At("TAIL"))
	private void handle(Entity entity, boolean ignoreCurrent, CallbackInfo ci) {
		this.worker.handleFluid(entity, entity.getFluidInteractionBox(), ignoreCurrent);
	}




	@Unique TriState vanillaKeys = TriState.DEFAULT;
	//TODO: remove after https://github.com/neoforged/NeoForge/pull/3249/changes
	//https://github.com/neoforged/NeoForge/pull/3303
	@Unique
	private FluidTracker getTracker(BlockInPlayer2 block) {
		if (vanillaKeys.isFalse()) {
			return trackerByFluid.computeIfAbsent(block.getBlockState().getFluidState().getType().getFluidType(), _ ->
					FluidTracker.of(new EntityFluidInteraction.Tracker()));

		} else if (vanillaKeys.isTrue()) {
			return byTag(block);
		}


		if (this.trackerByFluid.isEmpty()) {
			vanillaKeys = TriState.FALSE;
			return getTracker(block);
		}
		for (Object key : this.trackerByFluid.keySet()) {
			if (key instanceof FluidType) {
				vanillaKeys = TriState.FALSE;
				return getTracker(block);
			} else break;
		}
		vanillaKeys = TriState.TRUE;
		return getTracker(block);
	}

	@Unique
	private FluidTracker byTag(BlockInPlayer2 block) {
		for(Map.Entry<?, FluidTracker> entry : trackerByFluid.entrySet()) {
			Object tag = entry.getKey();
			if (tag instanceof TagKey<?> tag2 && block.getBlockState().getFluidState().is((TagKey<Fluid>) tag2)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
