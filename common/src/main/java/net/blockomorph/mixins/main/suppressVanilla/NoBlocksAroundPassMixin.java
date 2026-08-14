package net.blockomorph.mixins.main.suppressVanilla;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1001)
public class NoBlocksAroundPassMixin {

	@ModifyReturnValue(method = "noBlocksAround", at = @At("RETURN"))
	private boolean checkPlayersBlocks(boolean original, Entity player) {
		if (original) {
			AABB boundingBox = player.getBoundingBox();
			double margin = 0.0625;
			PlayersStorage storage = PlayersStorage.ofLevel(player.level());
			try (storage) {
				double minX = boundingBox.minX - margin;
				double minY = boundingBox.minY - margin - 0.55;
				double minZ = boundingBox.minZ - margin;
				double maxX = boundingBox.maxX + margin;
				double maxY = boundingBox.maxY + margin;
				double maxZ = boundingBox.maxZ + margin;
				for (Player morph : storage.findMorphedPlayers(player, minX, minY, minZ, maxX, maxY, maxZ)) {
					var cursor = PlayerAccessor.of(morph).getManager().getCursor3D();
					try (cursor) {
						for (BlockInPlayer2 ignored : cursor.forAllBlocks(minX, minY, minZ, maxX, maxY, maxZ))
							return true;
					}
				}
			}
		}
		return false;
	}
}
