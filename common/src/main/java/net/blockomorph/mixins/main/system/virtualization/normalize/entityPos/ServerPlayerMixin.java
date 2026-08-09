package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerPlayer.class, priority = 999)
public class ServerPlayerMixin {

	@Shadow private ServerPlayer.RespawnConfig respawnConfig;

	@FastInject(method = "getRespawnConfig", at = @At("HEAD"))
	private Object norm() {
		return MorphNormalizer.normalizeSpawnpoint(this.respawnConfig);
	}
}
