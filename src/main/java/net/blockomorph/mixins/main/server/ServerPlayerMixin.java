package net.blockomorph.mixins.main.server;

import com.mojang.authlib.GameProfile;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    @Shadow private boolean respawnForced;
    @Shadow public abstract boolean startRiding(Entity p_277395_, boolean p_278062_);
    @Shadow public abstract void stopRiding();

    public ServerPlayerMixin(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_) {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @ModifyVariable(method = "setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V", at = @At("HEAD"))
    private BlockPos modifyBlockPos(BlockPos originalPos) {
        AtomicReference<BlockPos> newPos = new AtomicReference<>(originalPos);
        InPlayerBlockPos.check(originalPos, (pl, realPos) -> {
            newPos.set(BlockPos.containing(MorphUtils.getRealBlockPos(pl, realPos)));
        }, () -> newPos.set(null), this.level());
        return newPos.get();
    }

    @Inject(method = "setRespawnPosition", at = @At("TAIL"))
    public void force(ResourceKey<Level> p_9159_, BlockPos pos, float p_9161_, boolean p_9162_, boolean p_9163_, CallbackInfo ci) {
        if (pos != null && InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
            this.respawnForced = true;
        }
    }

    @Inject(method = "startSleeping", at = @At("TAIL"))
    public void bound(BlockPos pos, CallbackInfo ci) {
        InPlayerBlockPos.check(pos, (pl, realPos) -> {
            this.startRiding(pl.player(), true);
        }, null, this.level());
    }

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    public void unbound(boolean p_9165_, boolean p_9166_, CallbackInfo ci) {
        this.getSleepingPos().ifPresent((blockpos) -> {
            if (InPlayerBlockPos.isMorphedPlayerX(blockpos.getX())) {
                this.stopRiding();
            }
        });
    }

    @Inject(method = "isReachableBedBlock", at = @At("HEAD"), cancellable = true)
    public void realInRange(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
            Vec3 real = InPlayerBlockPos.checkOnReal(Vec3.atBottomCenterOf(pos));
            cir.setReturnValue(Math.abs(this.getX() - real.x()) <= 3.0D && Math.abs(this.getY() - real.y()) <= 2.0D && Math.abs(this.getZ() - real.z()) <= 3.0D);
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    public void restoreBlockMorphData(ServerPlayer old, boolean fromEnd, CallbackInfo ci) {
        PlayerAccessor pl = PlayerAccessor.of(this);
        PlayerAccessor oldPl = PlayerAccessor.of(old);
        if (!fromEnd && (Boolean) Config.getInstance().getValue("playerDieAfterDestroy")) {
            pl.applyBlockMorph(Blocks.AIR.defaultBlockState(), null);
            for (InPlayerBlockPos pos : oldPl.getUpdates()) {
                pl.prepareSync(pos);
            }
        } else {
            pl.loadBlockData(oldPl.saveBlockData(false), null);
        }
    }
}
