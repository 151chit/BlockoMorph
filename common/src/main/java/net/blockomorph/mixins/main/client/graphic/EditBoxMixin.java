package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.screens.utils.ListenerEditBox;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {

	@Inject(method = "isBordered", at = @At("HEAD"), cancellable = true)
	private void isBordered(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ListenerEditBox box) {
			cir.setReturnValue(box.isBordered());
		}
	}
}
