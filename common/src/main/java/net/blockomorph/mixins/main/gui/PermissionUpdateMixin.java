package net.blockomorph.mixins.main.gui;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.screens.utils.ConfigSyncListener;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PermissionUpdateMixin {

	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void onOpUpdate(ClientboundEntityEventPacket packet, CallbackInfo ci, @Local Entity entity) {
		if (entity == GuiUtils.MC.player && GuiUtils.MC.screen instanceof ConfigSyncListener sc) {
			sc.onOperatorRightsChanged();
		}
	}
}
