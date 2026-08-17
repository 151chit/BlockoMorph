package net.blockomorph.mixins.main.system.virtualization.normalize.level.phys;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClipBlockStateContext.class)
public class ClipBlockStateContextMixin {
	@Mutable @Shadow @Final private Vec3 from;
	@Mutable @Shadow @Final private Vec3 to;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void normalize(CallbackInfo ci) {
		this.from = MorphNormalizer.normalize(this.from);
		this.to = MorphNormalizer.normalize(this.to);
	}
}
