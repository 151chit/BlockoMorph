package net.blockomorph.mixins.main.system.virtualization.normalize.level.chunks;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin {

	@FastInject(method = "inBlockTickingRange", at= @At("HEAD"))
	private byte yes(long key) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(ChunkPos.getX(key)))
			return 1;
		return 0;
	}
}
