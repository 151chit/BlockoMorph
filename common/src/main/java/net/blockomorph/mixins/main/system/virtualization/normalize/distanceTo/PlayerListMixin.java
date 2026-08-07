package net.blockomorph.mixins.main.system.virtualization.normalize.distanceTo;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Shadow @Final private List<ServerPlayer> players;

	@FastInject(method = "broadcast", at = @At("HEAD"))
	private boolean norm(Player except, double x, double y, double z, double range, ResourceKey<Level> dimension, Packet<?> packet) {
		return MorphNormalizer.normalizePacketSendPosAndSend(this.players, except != null ? except.getId() : -1, x, y, z, range, dimension, packet);
	}
}
