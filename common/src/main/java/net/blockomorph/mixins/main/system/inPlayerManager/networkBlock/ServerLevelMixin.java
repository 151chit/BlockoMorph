package net.blockomorph.mixins.main.system.inPlayerManager.networkBlock;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

	@FastInject(method = "blockEvent", at = @At(value = "HEAD"))
	public boolean blockEventHandle(BlockPos pos, Block block, int a, int b) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null) {
			pl.getManager().getNetworkManager().enqueueBlockEventData(new BlockEventData(pos, block, a, b));
			return false;
		}
		return true;
	}
}