package net.blockomorph.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.User;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;
import java.util.Optional;
import java.util.Random;

@Mixin(User.class) //FOR DEBUG ONLY!!!
public abstract class NickMixinMixin {
   String nick;
   private static boolean init;
   
   @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
   public void getNick(CallbackInfoReturnable<String> cir) {
   	  //if (true) return;
   	  if (!init) {
   	  	int length = 10;
        this.nick = generateRandomString(length);
        init = true;
   	  }
   	  cir.setReturnValue(nick);
   }

   private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char randomChar = (char) ('a' + random.nextInt(26));
            sb.append(randomChar);
        }
        return sb.toString();
    }

}


