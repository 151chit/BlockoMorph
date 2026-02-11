package net.blockomorph.mixins.neoforge;

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
			"tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
	}, at = @At("STORE"))
	private int changeLight(int i, BlockAndTintGetter level) {
		if (level instanceof ClientLevelAccessor acc && acc.specialRenderingMode()) {
			return LightTexture.pack(15, 15);
		}
		return i;
	}
}
