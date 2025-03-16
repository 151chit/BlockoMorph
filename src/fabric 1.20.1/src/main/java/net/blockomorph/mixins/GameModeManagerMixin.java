package net.blockomorph.mixins;

import net.blockomorph.utils.GamemodeAccessor;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


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
