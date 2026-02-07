package net.blockomorph.mixins.fabric;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.blockomorph.screens.utils.GuiBlockRenderState;
import net.blockomorph.screens.utils.GuiBlockRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiRenderer.class)
public abstract class GuiRenderMixin {

	@Shadow @Final
	GuiRenderState renderState;

	private final List<GuiBlockRenderer> last = new ArrayList<>();

	@Shadow @Final
	private MultiBufferSource.BufferSource bufferSource;

	@Inject(method = "preparePictureInPictureState", at = @At("HEAD"), cancellable = true)
	public <T extends PictureInPictureRenderState> void run(T pictureInPictureRenderState, int i, CallbackInfo ci) {
		if (pictureInPictureRenderState instanceof GuiBlockRenderState state) {
			ci.cancel();
			GuiBlockRenderer gui = new GuiBlockRenderer(this.bufferSource);
			this.last.add(gui);
			gui.prepare(state, this.renderState, i);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	public void render(GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
		this.last.forEach(PictureInPictureRenderer::close);
		this.last.clear();
	}

	@Inject(method = "close", at = @At("TAIL"))
	public void close(CallbackInfo ci) {
		this.last.forEach(PictureInPictureRenderer::close);
		this.last.clear();
	}
}
