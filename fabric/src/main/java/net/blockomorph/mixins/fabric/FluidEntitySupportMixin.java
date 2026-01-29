package net.blockomorph.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.AbstractMap;
import java.util.Set;

@Mixin(Entity.class)
public abstract class FluidEntitySupportMixin {
	@Shadow private Vec3 position;
	@Shadow @Final private Set<TagKey<Fluid>> fluidOnEyes;
	@Shadow public abstract boolean isPushedByFluid();

	@Shadow protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;

	@Inject(method = "updateFluidHeightAndDoFluidPushing", at = @At(value = "TAIL"), cancellable = true)
	public void handleFluid(TagKey<Fluid> tagKey, double fluidMotionScale, CallbackInfoReturnable<Boolean> cir) {
		boolean isPushedByFluid = this.isPushedByFluid();
		var map = CommonPlatformUtils.handleFluidDetection((Entity) (Object)this,
				tag -> isPushedByFluid,
				fluidState -> fluidState.is(tagKey),
				fluidState -> tagKey);
		if (!map.isEmpty()) cir.setReturnValue(true);
		CommonPlatformUtils.calculateLiquidsPower((Entity) (Object)this, map,
				tag -> fluidMotionScale,
				(tag, height) -> this.fluidHeight.put(tag, height.doubleValue()));
	}

	@WrapOperation(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
	public FluidState getRealBlock(Level instance, BlockPos blockPos, Operation<FluidState> original) {
		FluidState fluidState = original.call(instance, blockPos);
		if (!fluidState.is(FluidTags.WATER)) {
			AbstractMap.SimpleEntry<PlayerAccessor, BlockInPlayer2> fluidState1 = MorphUtils.getLiquidOnPos((Entity) (Object) this, this.position);
			if (fluidState1 != null) return fluidState1.getValue().getBlockState().getFluidState();
		}
		return fluidState;
	}

	@Inject(method = "updateFluidOnEyes", at = @At("TAIL"))
	public void getLiquid(CallbackInfo ci) {
		CommonPlatformUtils.handleUpdateFluidOnEyes((Entity) (Object)this, fluidState -> {
			fluidState.getTags().forEach(tag -> this.fluidOnEyes.add(tag));
		});
	}
}
