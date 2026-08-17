package net.blockomorph.mixins.neoforge;

import net.blockomorph.core.coords.MorphedPlayerSection;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ServerChunkCache.class, priority = 20_000)
public class ServerChunkCacheMixin {
	@ModifyVariable(require = 1, method = {
			"removeRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;Z)V",
			"addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;Z)V"
	}, at = @At("HEAD"), remap = false)
	public ChunkPos changePosRemove(ChunkPos orig) {
		Player player = MorphedPlayerSection.checkOnBoundPlayer(orig);
		if (player != null) return player.chunkPosition();
		return orig;
	}
}
