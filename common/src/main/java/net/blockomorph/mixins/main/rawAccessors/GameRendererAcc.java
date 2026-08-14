package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererAcc implements Accessors.GameRendererAccessor {
	@Shadow protected abstract boolean shouldRenderBlockOutline();

	@Override
	public boolean shouldRenderOutline$bm() {
		return this.shouldRenderBlockOutline();
	}
}
