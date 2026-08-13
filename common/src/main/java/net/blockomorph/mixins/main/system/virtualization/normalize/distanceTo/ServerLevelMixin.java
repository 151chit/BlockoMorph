package net.blockomorph.mixins.main.system.virtualization.normalize.distanceTo;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@Shadow @Final private MinecraftServer server;

	@FastInject(method = "destroyBlockProgress", at = @At("HEAD"))
	private boolean overrideDistance(int id, BlockPos blockPos, int progress) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(blockPos.getX()))
			return MorphNormalizer.normalizePacketSendPosAndSend(this.server.getPlayerList().getPlayers(), id, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
					32, this.getThis().dimension(), new ClientboundBlockDestructionPacket(id, blockPos, progress));
		return true;
	}

	@Unique
	private Level getThis() {
		return (Level) (Object) this;
	}
}
