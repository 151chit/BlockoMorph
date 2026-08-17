package net.blockomorph.mixins.main.system.inPlayerManager.chairController.detector;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.misc.chairController.ChairUtils;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 999)
public class EntityMixin implements ChairUtils.InPlayerSpawnedEntity {
	@Shadow private Level level;
	@Unique private PlayerAccessor playerThisTick;

	@Inject(method = "setPosRaw", at = @At("HEAD"))
	private void capture(double x, double y, double z, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
			this.playerThisTick = InPlayerBlockPos.findPlayer(Mth.floor(x), Mth.floor(z));
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void eraseCapture(CallbackInfo ci) {
		this.playerThisTick = null;
	}

	@Override
	public PlayerAccessor getLastOwnerPlayer() {
		return this.playerThisTick;
	}

	@FastInject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"))
	private byte check(Entity entityToRide, boolean force) {
		if (!this.level.isClientSide() && this instanceof PlayerAccessor pl && ChairUtils.checkChairCandidateAndRun(entityToRide, pl)) {
			return 1;
		}
		return 0;
	}
}
