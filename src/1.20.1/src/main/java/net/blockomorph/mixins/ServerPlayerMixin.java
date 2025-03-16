package net.blockomorph.mixins;

import com.mojang.authlib.GameProfile;
import net.blockomorph.network.blockFix.ClientBoundBlockEventPacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.BlockEntityAccessor;
import net.blockomorph.utils.use.UseController;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_) {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "openTextEdit", at = @At(value = "HEAD"), cancellable = true)
    public void open(SignBlockEntity signBlockEntity, boolean front, CallbackInfo ci) {
        if (signBlockEntity instanceof BlockEntityAccessor acc) {
            UseController ctr = acc.getController();
            if (ctr != null) {
                ci.cancel();
                MorphUtils.sendPlayer(ClientBoundBlockEventPacket.levelEvent(ctr.getOwner().getId(), ctr.getOffset(), -1, front ? 1 : 0), (ServerPlayer) (Object) this);
            }
        }
    }
}
