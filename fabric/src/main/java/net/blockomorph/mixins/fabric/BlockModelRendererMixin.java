package net.blockomorph.mixins.fabric;

import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin {

	@ModifyVariable(ordinal = 3, method = {
			"tesselateWithoutAO",
	}, at = @At("STORE"))
	private int changeLight(int i, BlockAndTintGetter level) {
		if (level instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			return LightTexture.pack(15, 15);
		}
		return i;
	}
}
