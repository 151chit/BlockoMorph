package net.blockomorph.mixins.main.client.graphic.fluidFog;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(Camera.class)
public abstract class CameraMixin {

	@Shadow private boolean initialized;

	@Shadow private Level level;

	@Shadow
	public abstract Camera.NearPlane getNearPlane();

	@Shadow private Vec3 position;

	@Shadow private Entity entity;

	@Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
	public void getVanillaFluid(CallbackInfoReturnable<FogType> cir) {
		if (this.initialized && this.level instanceof ClientLevel lv) {
			Camera.NearPlane nearPlane = this.getNearPlane();
			for (Vec3 pos : Arrays.asList(nearPlane.getPointOnPlane(0, 0), nearPlane.getTopLeft(), nearPlane.getTopRight(), nearPlane.getBottomLeft(), nearPlane.getBottomRight())) {
				Vec3 position = this.position.add(pos);
				MorphUtils.doBlockInMorphedPlayerOnPos(this.entity, lv.entitiesForRendering(), position, ((pl, block) -> {
					if (block.getBlockState().is(Blocks.POWDER_SNOW)) {
						cir.setReturnValue(FogType.POWDER_SNOW);
					} else if (block.shouldDoFluidAction()) {
						FluidState fluidState = block.getBlockState().getFluidState();
						if (position.y < (MorphUtils.getRealBlockPos(pl, block.getOffset()).y + fluidState.getHeight(this.level, block.getPos()))) {
							if (fluidState.is(FluidTags.WATER)) {
								cir.setReturnValue(FogType.WATER);
							} else if (fluidState.is(FluidTags.LAVA)) {
								cir.setReturnValue(FogType.LAVA);
							}
						}
					}
				}));
			}
		}
	}
}
