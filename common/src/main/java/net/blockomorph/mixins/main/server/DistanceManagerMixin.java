package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin {

	@Inject(method = "inBlockTickingRange", at= @At("HEAD"), cancellable = true)
	private void yes(long l, CallbackInfoReturnable<Boolean> cir) {
		int chunkX = ChunkPos.getX(l);
		if (InPlayerBlockPos.isMorphedPlayerX(SectionPos.sectionToBlockCoord(chunkX))) {
			cir.setReturnValue(true);
		}
	}
}
