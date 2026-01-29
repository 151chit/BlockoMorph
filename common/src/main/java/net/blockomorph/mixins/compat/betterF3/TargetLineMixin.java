package net.blockomorph.mixins.compat.betterF3;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.accessors.compat.BaseModuleAccessor;
import net.blockomorph.utils.accessors.compat.DebugLineAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me.cominixo.betterf3.modules.TargetModule")
public class TargetLineMixin {
	@Unique private DebugLineAccessor blockLine;
	@Unique private DebugLineAccessor fluidLine;

	@Inject(method = "update", at = @At(value = "HEAD"))
	private void changeTarget(Minecraft client, CallbackInfo ci) {
		if (this instanceof BaseModuleAccessor acc) {
			if (this.blockLine == null || this.fluidLine == null) {
				for (DebugLineAccessor line : acc.getModules()) {
					if (line.id$blockomroph().equals("targeted_block")) {
						this.blockLine = line;
					} else if (line.id$blockomroph().equals("targeted_fluid")) {
						this.fluidLine = line;
					}
				}
			}
		}
	}

	@Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getTags()Ljava/util/stream/Stream;"))
	private void changeBlock(Minecraft client, CallbackInfo ci, @Local(ordinal = 0) HitResult blockHit) {
		this.doChange(this.blockLine, blockHit);
	}

	@Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getTags()Ljava/util/stream/Stream;"))
	private void changeFluid(Minecraft client, CallbackInfo ci, @Local(ordinal = 1) HitResult fluidHit) {
		this.doChange(this.fluidLine, fluidHit);
	}

	@Unique
	private void doChange(DebugLineAccessor line, HitResult hit) {
		if (line != null) {
			String out = GuiUtils.redirectBlockInfo(null, hit);
			if (out != null) {
				line.setValue$blockomorph(out);
			}
		}
	}
}
