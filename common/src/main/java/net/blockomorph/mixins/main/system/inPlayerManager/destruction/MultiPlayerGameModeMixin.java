package net.blockomorph.mixins.main.system.inPlayerManager.destruction;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {//i don't want store breaks by players ids
	@Shadow private BlockPos destroyBlockPos;

	@Inject(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;ABORT_DESTROY_BLOCK:Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;", opcode = Opcodes.GETSTATIC))
	private void abort(CallbackInfoReturnable<Boolean> cir) {
		if (this.destroyBlockPos != null &&
				InPlayerBlockPos.isMorphedPlayerBlockX(this.destroyBlockPos.getX()) && GuiUtils.MC.level != null && GuiUtils.MC.player != null) {
			GuiUtils.MC.level.destroyBlockProgress(GuiUtils.MC.player.getId(), this.destroyBlockPos, -1);
		}
	}
}
