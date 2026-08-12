package net.blockomorph.mixins.main.system.inPlayerManager.networkBlock;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerChunkCache.class)
public class ServerChunkCacheMixin {

	@FastInject(method = "blockChanged", at = @At(value = "HEAD"))
	public boolean redirectChange(BlockPos pos) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				pl.getManager().getNetworkManager().enqueueBlockNetworkUpdate(posIn);
				return false;
			}
		}
		return true;
	}
}