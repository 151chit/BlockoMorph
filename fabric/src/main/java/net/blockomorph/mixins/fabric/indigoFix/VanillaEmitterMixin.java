package net.blockomorph.mixins.fabric.indigoFix;

import net.blockomorph.core.render.renderers.DirectBlocksRenderer;
import net.blockomorph.core.render.renderers.async.PlayersAsyncBakersManager;
import net.blockomorph.utils.side.Side;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.renderer.VanillaBlockModelPartEncoder;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(VanillaBlockModelPartEncoder.class)
public class VanillaEmitterMixin {
	@Unique
	private static final RenderMaterial MATERIAL = IndigoRenderer.INSTANCE.materialFinder().shadeMode(ShadeMode.VANILLA).find();
	@Unique
	private static final RenderMaterial NOT_AO_MATERIAL = IndigoRenderer.INSTANCE.materialFinder().shadeMode(ShadeMode.VANILLA).ambientOcclusion(TriState.FALSE).find();

	@ModifyVariable(method = "emitQuads", at = @At("STORE"))
	private static RenderMaterial redirectStaticFields(RenderMaterial defaultMaterial, BlockModelPart part) {
		Thread thread = Thread.currentThread();
		if (thread instanceof PlayersAsyncBakersManager.MorphBakerThread || (Side.CLIENT.isThisSide(thread) && DirectBlocksRenderer.RENDER_PASS)) {
			return part.useAmbientOcclusion() ? MATERIAL : NOT_AO_MATERIAL;
		}
		return defaultMaterial;
	}
}
