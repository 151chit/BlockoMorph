package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.GamemodeAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;


@Mixin(MultiPlayerGameMode.class)
public abstract class GameModeManagerMixin implements GamemodeAccessor {
   @Shadow private int destroyDelay;
   
   @Inject(method = "destroyBlock", at = @At(value = "TAIL"), cancellable = true)
   public void stopMine(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    Minecraft mc = Minecraft.getInstance();
    MorphUtils.onPick();
    mc.gameRenderer.pick(1.0F);
   }

   public int getDelay() {
   	return this.destroyDelay;
   }

   public void setDelay(int del) {
   	this.destroyDelay = del;
   }
}
