package net.blockomorph.mixins.fabric.indigoFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.render.renderers.DirectBlocksRenderer;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.blockomorph.utils.side.Side;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(VanillaModelEncoder.class) @Debug(export = true)//todo
public class VanillaEmitterMixin {
	@Unique
	private static final RenderMaterial MATERIAL = IndigoRenderer.INSTANCE.materialFinder().shadeMode(ShadeMode.VANILLA).find();
	@Unique
	private static final RenderMaterial NOT_AO_MATERIAL = IndigoRenderer.INSTANCE.materialFinder().shadeMode(ShadeMode.VANILLA).ambientOcclusion(TriState.FALSE).find();

	@ModifyVariable(method = "emitBlockQuads", at = @At("STORE"))
	private static RenderMaterial redirectStaticFields(RenderMaterial defaultMaterial, @Local(argsOnly = true) BakedModel model) {
		Thread thread = Thread.currentThread();
		if (thread instanceof PlayersAsyncBakersManager.MorphBakerThread || (Side.CLIENT.isThisSide(thread) && DirectBlocksRenderer.RENDER_PASS)) {
			return model.useAmbientOcclusion() ? MATERIAL : NOT_AO_MATERIAL;
		}
		return defaultMaterial;
	}

	@ModifyArg(method = "emitItemQuads", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;fromVanilla(Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;Lnet/minecraft/core/Direction;)Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;"))
	private static RenderMaterial injectCustomMaterial(RenderMaterial original) {
		Thread thread = Thread.currentThread();
		if (thread instanceof PlayersAsyncBakersManager.MorphBakerThread || (Side.CLIENT.isThisSide(thread) && DirectBlocksRenderer.RENDER_PASS)) {
			return MATERIAL;
		}
		return original;
	}
}
