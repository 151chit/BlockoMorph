package net.blockomorph.mixins.main.system.virtualization.normalize.level.chunks;

import net.blockomorph.core.coords.MorphedPlayerSection;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ChunkMap.class, priority = 20_000)
public class ChunkMapMixin {

	@ModifyVariable(method = {"getPlayers", "getPlayersCloseForSpawning"}, at = @At(value = "HEAD"))
	public ChunkPos redirectChunkPos(ChunkPos value) {
		Player pl = MorphedPlayerSection.checkOnBoundPlayer(value);
		if (pl != null) return pl.chunkPosition();
		return value;
	}
}
