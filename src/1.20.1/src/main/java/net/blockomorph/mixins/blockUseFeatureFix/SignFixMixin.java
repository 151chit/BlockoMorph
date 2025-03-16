package net.blockomorph.mixins.blockUseFeatureFix;

import net.blockomorph.network.blockFix.ServerBoundSignUpdatePacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.BlockEntityAccessor;
import net.blockomorph.utils.use.UseController;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public class SignFixMixin {
    @Shadow @Final private SignBlockEntity sign;
    @Shadow @Final private boolean isFrontText;
    @Shadow @Final private String[] messages;

    @Inject(method = "removed", at = @At(value = "HEAD"), cancellable = true)
    public void fix(CallbackInfo ci) {
        if (this.sign instanceof BlockEntityAccessor acc) {
            UseController ctr = acc.getController();
            if (ctr != null) {
                ci.cancel();
                MorphUtils.sendServer(new ServerBoundSignUpdatePacket(
                        ctr.getOwner().getId(),
                        ctr.getOffset(),
                        this.isFrontText,
                        this.messages[0],
                        this.messages[1],
                        this.messages[2],
                        this.messages[3])
                );
            }
        }
    }
}
