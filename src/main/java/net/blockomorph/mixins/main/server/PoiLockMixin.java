package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoiManager.class)
public class PoiLockMixin {

    @Inject(method = "add", at = @At(value = "HEAD"), cancellable = true)
    public void reject(BlockPos pos, Holder<PoiType> holder, CallbackInfo ci) {
        if (InPlayerBlockPos.isMorphedPlayerX(pos.getX()))
            ci.cancel();
    }

    @Inject(method = "remove", at = @At(value = "HEAD"), cancellable = true)
    public void reject(BlockPos pos, CallbackInfo ci) {
        if (InPlayerBlockPos.isMorphedPlayerX(pos.getX()))
            ci.cancel();
    }
}
