package net.blockomorph.mixins.main.system.morphState.playerFeatures.bed;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

	protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
		super(type, level);
	}

	@FastInject(method = "updatePlayerPose", at = @At("HEAD"))
	public boolean checkModedBeds() {
		Optional<BlockPos> sleepPos = this.getSleepingPos();
		if (sleepPos.isPresent() && InPlayerBlockPos.isMorphedPlayerBlockX(sleepPos.get().getX())) {
			this.setPose(Pose.SLEEPING);
			return false;
		}
		return true;
	}
}
