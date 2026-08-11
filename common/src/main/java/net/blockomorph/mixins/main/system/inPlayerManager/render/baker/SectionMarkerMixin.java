package net.blockomorph.mixins.main.system.inPlayerManager.render.baker;

import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.MorphedPlayerSection;
import net.blockomorph.core.coords.blockPosPointer.BlockPosBounds;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class SectionMarkerMixin {

	@FastInject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
	private boolean update(int sectionX, int sectionY, int sectionZ, boolean playerChanged) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(sectionX)) {
			long pos = MorphedPlayerSection.fromMorphedChunk(sectionX, sectionZ);
			if (pos != -1) {
				PlayerAccessor pl = PlayerAccessor.of(BlockPosBounds.getPlayerBySection(pos));
				if (pl != null && pl.getManager() instanceof ClientInPlayerManager mn) {
					mn.getSectionsDirtyMarker().markSectionDirty(sectionX, sectionY, sectionZ, true);
				}
			}
			return false;
		}
		return true;
	}

	@FastInject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"))
	private boolean update(BlockPos pos, boolean playerChanged) {
		return this.checkBlocks(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
	}

	@FastInject(method = "setBlocksDirty", at = @At("HEAD"))
	private boolean update(int x0, int y0, int z0, int x1, int y1, int z1) {
		return this.checkBlocks(x0, y0, z0, x1, y1, z1);
	}

	@Unique
	private boolean checkBlocks(int x0, int y0, int z0, int x1, int y1, int z1) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(Mth.lerp(0.5, x0, x1))) {
			PlayerAccessor pl = InPlayerBlockPos.findPlayer(Mth.floor(Mth.lerp(0.5, x0, x1)), Mth.floor(Mth.lerp(0.5, z0, z1)));
			if (pl != null && pl.getManager() instanceof ClientInPlayerManager mn) for (int z = z0 - 1; z <= z1 + 1; z++) {
				for (int x = x0 - 1; x <= x1 + 1; x++) {
					for (int y = y0 - 1; y <= y1 + 1; y++) {
						int offX = x - mn.getZeroKey().getX();
						int offY = y - mn.getZeroKey().getY();
						int offZ = z - mn.getZeroKey().getZ();
						int index = InPlayerBlockPos.asInt(offX, offY, offZ);
						if (index != -1) {
							mn.getSectionsDirtyMarker().markSectionDirty(index);
						}
					}
				}
			}
			return false;
		}
		return true;
	}
}
