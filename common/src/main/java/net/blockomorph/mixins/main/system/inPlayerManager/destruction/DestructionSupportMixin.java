package net.blockomorph.mixins.main.system.inPlayerManager.destruction;

import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class DestructionSupportMixin {

	@FastInject(method = "destroyBlockProgress", at = @At("HEAD"))
	private boolean redirect(int id, BlockPos pos, int progress) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null && pl.getManager() instanceof ClientInPlayerManager mn) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				mn.getDestructionHandler().markBlockDestroying(id, posIn, progress);
				return false;
			}
		}
		return true;
	}
}
