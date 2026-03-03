package net.blockomorph.mixins.main.server;

import com.mojang.authlib.GameProfile;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ForceLevelChanger;
import net.blockomorph.utils.accessors.ServerPlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements ServerPlayerAccessor {

	public ServerPlayerMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	@ModifyVariable(method = "setRespawnPosition", at = @At("HEAD"))
	private ServerPlayer.RespawnConfig modifyBlockPos(ServerPlayer.RespawnConfig originalPos) {
		if (originalPos == null) return null;
		LevelData.RespawnData data = originalPos.respawnData();
		BlockPos newPos = InPlayerBlockPos.checkOnReal(data.pos());
		return new ServerPlayer.RespawnConfig(new LevelData.RespawnData(new GlobalPos(data.dimension(), newPos), data.yaw(), data.pitch()), newPos != originalPos.respawnData().pos() || originalPos.forced());
	}

	@Inject(method = "startSleeping", at = @At("TAIL"))
	public void bound(BlockPos pos, CallbackInfo ci) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			this.startRiding(pl.player(), true, false);
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

	@Inject(method = "die", at = @At("HEAD"))
	public void dead(DamageSource damageSource, CallbackInfo ci) {
		if (PlayerAccessor.of(this).isFullActive() && !damageSource.is(MorphUtils.PLAYER_DESTROYED) && !damageSource.is(MorphUtils.PLAYER_DESTROYED_NULL)) {
			PlayerAccessor.of(this).getBlocksData2().values().forEach(block -> {
				this.level().levelEvent(2001, block.getPos(), Block.getId(block.getBlockState()));
			});
		}
	}

	@Override
	public void dropAllDeathLoot$blockomorph(ServerLevel lv, DamageSource dm) {
		this.dropAllDeathLoot(lv, dm);
	}

	@Inject(method = "restoreFrom", at = @At("TAIL"))
	public void restoreBlockMorphData(ServerPlayer old, boolean fromEnd, CallbackInfo ci) {
		PlayerAccessor pl = PlayerAccessor.of(this);
		PlayerAccessor oldPl = PlayerAccessor.of(old);
		if (!fromEnd && Config.get().playerDieAfterDestroy.getValue()) {
			pl.applyBlockMorph(Blocks.AIR.defaultBlockState(), null, BannedBlock.Source.SYSTEM);
			for (InPlayerBlockPos pos : oldPl.getUpdates()) {
				pl.prepareSync(pos);
			}
		} else {
			pl.loadBlockData(oldPl.saveBlockData(false), null, true);
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	public void hurt(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
		if (MorphUtils.onPlayerAttacked(damageSource, this)) cir.setReturnValue(false);
	}

	@Inject(method = "setServerLevel", at= @At("TAIL"))
	private void changeForBE(ServerLevel serverLevel, CallbackInfo ci) {
		PlayerAccessor.of(this).getBlocksData2().values().forEach(block -> {
			if (block.getBlockEntity() != null) {
				ForceLevelChanger.of(block.getBlockEntity()).forceLevelChange(serverLevel);
			}
		});
	}
}
