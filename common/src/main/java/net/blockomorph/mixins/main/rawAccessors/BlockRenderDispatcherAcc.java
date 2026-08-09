package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherAcc implements Accessors.BlockRendererDispatcherAccessor {
	@Shadow private @Nullable LiquidBlockRenderer liquidBlockRenderer;

	@Override
	public LiquidBlockRenderer getFluidRenderer$bm() {
		return this.liquidBlockRenderer;
	}
}
