package net.blockomorph.mixins.main.register.keyBindings;

import net.blockomorph.input.KeyBindings;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "handleKeybinds", at = @At("TAIL"))
	private void handle(CallbackInfo ci) {
		KeyBindings.handle();
	}
}
