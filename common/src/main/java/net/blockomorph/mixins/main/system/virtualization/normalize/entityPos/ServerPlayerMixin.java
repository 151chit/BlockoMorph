package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerPlayer.class, priority = 999)
public class ServerPlayerMixin {
	@Shadow @Nullable private BlockPos respawnPosition;

	@ModifyReturnValue(method = "getRespawnPosition", at = @At("RETURN"))
	private BlockPos norm(BlockPos original) {
		return MorphNormalizer.normalizeSpawnpoint(original);
	}

	@ModifyReturnValue(method = "isRespawnForced", at = @At("RETURN"))
	private boolean norm(boolean original) {
		if (this.respawnPosition != null && InPlayerBlockPos.isMorphedPlayerBlockX(this.respawnPosition.getX())) {
			return true;
		}
		return original;
	}
}
