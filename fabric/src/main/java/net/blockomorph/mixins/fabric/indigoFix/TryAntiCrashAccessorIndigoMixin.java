package net.blockomorph.mixins.fabric.indigoFix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.utils.MorphUtils;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockModelLighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("UnstableApiUsage")
@Mixin(AltModelBlockRendererImpl.class)
public class TryAntiCrashAccessorIndigoMixin {
	@Unique private static boolean BROKEN;

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/mixin/client/indigo/renderer/BlockModelLighterAccessor;fabric_getCACHE()Ljava/lang/ThreadLocal;"))
	private ThreadLocal<BlockModelLighter.Cache> doForceRun(Operation<ThreadLocal<BlockModelLighter.Cache>> original) {
		if (BROKEN) return BlockModelLightAcc.cache$bm();
		try {
			return original.call();
		} catch (Throwable e) {
			BROKEN = true;
			MorphUtils.LOGGER.warn("Indigo renderer disabled, try run this with bypass");
			return BlockModelLightAcc.cache$bm();
		}
	}
}
