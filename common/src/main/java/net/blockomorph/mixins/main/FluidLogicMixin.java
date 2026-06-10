package net.blockomorph.mixins.main;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.FluidTracker;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(EntityFluidInteraction.class) @Debug(export = true)
public abstract class FluidLogicMixin {

	@Shadow @Final private Map<TagKey<Fluid>, ?> trackerByFluid;

	@Inject(method = "update", at = @At("RETURN"))
	private void update(Entity entity, boolean ignoreCurrent, CallbackInfo ci) {
		AABB box = entity.getFluidInteractionBox();
		if (box != null) {
			List<Entity> entities = entity.level().getEntities(entity, box, EntitySelector.NO_SPECTATORS);
			MutableObject<FluidTracker> lastTracker = new MutableObject<>();
			MutableObject<Fluid> lastFluid = new MutableObject<>();
			Vec3 entityPos = entity.position();
			double eyeY = entity.getEyeY();
			for (Entity playerCandidate : entities) {
				if (playerCandidate instanceof PlayerAccessor pl && pl.isFullActive()) {
					pl.getBlocksData2InArea(box, (_, block, realPos) -> {
						if (block.shouldDoFluidAction()) {
							FluidState fluidState = block.getBlockState().getFluidState();
							if (!fluidState.isEmpty()) {
								double fluidBottom = realPos.y;
								double fluidTop = fluidBottom + fluidState.getHeight(entity.level(), block.getPos());
								if (!(fluidTop < box.minY)) {
									Fluid fluid = fluidState.getType();
									if (lastFluid.get() != fluid) {
										FluidTracker tracker = this.findTrackerByFluid(fluidState);
										lastTracker.setValue(tracker);
									}
									FluidTracker tracker = lastTracker.get();
									if (tracker != null) {
										Vec3 corner = realPos.add(1);
										if (entityPos.z > realPos.z && entityPos.z() < corner.z &&
												entityPos.x > realPos.x && entityPos.x < corner.x &&
												eyeY >= fluidBottom && eyeY <= fluidTop) {
											tracker.eyeInside$blockomorph().setValue(true);
										}
										tracker.fluidHeight$blockomorph().setValue(
												Math.max(fluidTop - entity.getBoundingBox().minY, tracker.fluidHeight$blockomorph().doubleValue())
										);
										if (!ignoreCurrent) {
											Vec3 flow = fluidState.getFlow(entity.level(), block.getPos());
											if (tracker.fluidHeight$blockomorph().doubleValue() < 0.4) {
												flow = flow.scale(tracker.fluidHeight$blockomorph().doubleValue());
											}

											tracker.accumulateCurrent(flow);
										}
									}
								}
							}
						}
					});
				}
			}
		}
	}

	@Unique
	private FluidTracker findTrackerByFluid(FluidState fluidState) {
		for(Map.Entry<TagKey<Fluid>, ?> entry : this.trackerByFluid.entrySet()) {
			TagKey<Fluid> tag = entry.getKey();
			if (fluidState.is(tag)) {
				return FluidTracker.of(entry.getValue());
			}
		}
		return null;
	}
}
