package net.blockomorph.mixins.main.rawAccessors;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAcc {

	@Invoker("register")
	static RenderPipeline reg$bm(RenderPipeline pipeline) {
		throw new AssertionError();
	}

	@Accessor("BLOCK_SNIPPET")
	static RenderPipeline.Snippet blockSnippet$bm() {
		throw new AssertionError();
	}
}
