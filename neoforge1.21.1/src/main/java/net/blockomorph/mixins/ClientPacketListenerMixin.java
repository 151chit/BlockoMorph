package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.blockomorph.utils.PlayerAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

   @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"), cancellable = true)
   public void autoRespawn(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
   	  LocalPlayer player = Minecraft.getInstance().player;
   	  Entity entity = player.level().getEntity(packet.playerId());
   	  if (entity == player && ((PlayerAccessor)player).isActive()) {
   	  	  ci.cancel();
          player.respawn();
      }
   }

   
   
}
