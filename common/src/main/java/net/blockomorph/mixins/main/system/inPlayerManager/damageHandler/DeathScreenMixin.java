package net.blockomorph.mixins.main.system.inPlayerManager.damageHandler;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.screens.overlay.PlayerCrackOverlay;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
	@Shadow protected abstract void setButtonsActive(boolean isActive);
	@Unique private final GuiUtils guiUtils = new GuiUtils();

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
	private static MutableComponent checkMorph(String key, Operation<MutableComponent> original) {
		if (isMorphActive())
			return original.call("blockomorph.gui.vanilla.deathScreen.broken");
		return original.call(key);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void resetClock(CallbackInfo ci) {
		if (isMorphActive()) {
			this.setButtonsActive(true);
		}
	}

	@Inject(method = "renderBackground", at = @At("HEAD"))
	private void cracks(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (isMorphActive()) {
			this.guiUtils.setGuiGraphics(graphics, GuiUtils.MC.font, 0, 0, 0);
			PlayerCrackOverlay.renderCracks(this.guiUtils, 9, this.width, this.height);
		}
	}

	@Unique
	private static boolean isMorphActive() {
		return GuiUtils.MC.player != null && PlayerAccessor.of(GuiUtils.MC.player).isBlockomorphActive();
	}

	protected DeathScreenMixin(Component title) {
		super(title);
	}
}
