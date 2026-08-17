package net.blockomorph.mixins.main.system.morphState.playerFeatures.bed;

import com.mojang.authlib.GameProfile;
import net.blockomorph.core.misc.chairController.EntityChairController;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

	public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, blockPos, f, gameProfile);
	}

	@Inject(method = "startSleeping", at = @At("TAIL"))
	public void boundForBed(BlockPos bedPosition, CallbackInfo ci) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(bedPosition);
		if (pl != null && this instanceof EntityChairController.ChairedEntity ch) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(bedPosition);
			if (posIn != -1) {
				Vec3 boundPos = new Vec3(InPlayerBlockPos.getX(posIn) + 0.5, InPlayerBlockPos.getY(posIn) + 0.5625, InPlayerBlockPos.getZ(posIn) + 0.5);
				ch.getController().boundForPlayer(pl.player().getUUID(), boundPos,
						new EntityChairController.BlockCondition(InPlayerBlockPos.decode(posIn), pl.getBlockState(posIn).getBlock()), null);
			}
		}
	}

	@Inject(method = "stopSleepInBed", at = @At("HEAD"))
	public void unbound(boolean forcefulWakeUp, boolean updateLevelList, CallbackInfo ci) {
		Optional<BlockPos> sleepPos = this.getSleepingPos();
		if (sleepPos.isPresent() && this instanceof EntityChairController.ChairedEntity ch) {
			ch.getController().unbound();
		}
	}

	@ModifyVariable(method = "isReachableBedBlock", at = @At("STORE"))
	public Vec3 realInRange(Vec3 bedBlockCenter, BlockPos bedBlockPos) {
		return MorphNormalizer.normalize(bedBlockCenter);
	}
}
