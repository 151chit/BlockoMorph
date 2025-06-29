package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.coords.PlayerMorphedSection;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {
	@Shadow @Final ServerLevel level;

	@ModifyVariable(method = {
			"getPlayers",
			"getPlayersCloseForSpawning"
	}, at = @At(value = "HEAD"))
	public ChunkPos redirectChunkPos(ChunkPos value) {
		if (InPlayerBlockPos.isMorphPlayerChunk(value)) {
			Player pl = PlayerMorphedSection.getPlayerByChunkPos(value, this.level.isClientSide);
			if (pl != null)
				return pl.chunkPosition();
		}
		return value;
	}
}
