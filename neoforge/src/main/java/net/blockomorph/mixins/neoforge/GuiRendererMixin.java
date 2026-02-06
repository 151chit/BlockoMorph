package net.blockomorph.mixins.neoforge;

import net.blockomorph.screens.utils.GuiBlockRenderState;
import net.blockomorph.screens.utils.GuiBlockRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

	@ModifyVariable(method = "<init>", at = @At("HEAD"))
	private static List<PictureInPictureRendererRegistration<?>> getList(List<PictureInPictureRendererRegistration<?>> list) {
		List<PictureInPictureRendererRegistration<?>> list2 = new ArrayList<>(list);
		list2.add(new PictureInPictureRendererRegistration<>(GuiBlockRenderState.class, GuiBlockRenderer::new));
		return List.copyOf(list2);
	}
}
