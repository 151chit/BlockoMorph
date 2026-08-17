package net.blockomorph.mixins.main.system.virtualization.normalize.level.chunks;

import net.blockomorph.core.coords.MorphedPlayerSection;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ServerChunkCache.class, priority = 20_000)
public class ServerChunkCacheMixin {

	@ModifyVariable(require = 1, method = {
			"updateChunkForced",
			"getChunkDebugData"
	}, at = @At("HEAD"))
	public ChunkPos changePos(ChunkPos orig) {
		Player player = MorphedPlayerSection.checkOnBoundPlayer(orig);
		if (player != null) return player.chunkPosition();
		return orig;
	}
}
