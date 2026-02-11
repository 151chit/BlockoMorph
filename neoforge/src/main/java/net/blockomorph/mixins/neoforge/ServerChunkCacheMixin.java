package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {

	@Shadow @Final public ServerLevel level;

	@ModifyVariable(require = 1, method = {
			"removeRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;Z)V",
			"addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;Z)V"
	}, at = @At("HEAD"), remap = false)
	public ChunkPos changePosRemove(ChunkPos orig) {
		return MorphUtils.getChangedChunk(orig, this.level.isClientSide);
	}
}
