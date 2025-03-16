package net.blockomorph.mixins;

import net.blockomorph.utils.accessors.BlockEntityAccessor;
import net.blockomorph.utils.use.UseController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements BlockEntityAccessor {
    @Unique
    UseController controller;

    @Inject(method = "getBlockPos", at = @At(value = "HEAD"), cancellable = true)
    public void getPos(CallbackInfoReturnable<BlockPos> cir) {
        if (this.controller != null) cir.setReturnValue(this.controller.getOwner().blockPosition());
    }

    public void setUseController(UseController ctr) {
        this.controller = ctr;
    }

    public UseController getController() {
        return this.controller;
    }
}
