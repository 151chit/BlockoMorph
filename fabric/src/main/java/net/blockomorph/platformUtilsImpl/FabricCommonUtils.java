package net.blockomorph.platformUtilsImpl;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.network.MainPacket;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.blockomorph.utils.tnt.TntSpawnLevel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

public class FabricCommonUtils implements CommonPlatformUtils {

	@Override
	public void litTnt(TntBlock blockVanilla, TntSpawnLevel lv, BlockInPlayer2 block, PlayerAccessor pl) {
		blockVanilla.defaultBlockState().handleNeighborChanged(lv, block.getPos(), Blocks.REDSTONE_BLOCK, null, false);
	}

	@Override
	public TntSpawnLevel createLevel(Level orig, BlockState need) {
		return new TntSpawnLevel(orig, need) {
			@Override
			public Collection<EnderDragonPart> dragonParts() {
				return realLevel.dragonParts();
			}
		};
	}

	@Override
	public void loadNbtToBlockEntityOnClient(BlockEntity blockEntity, ClientBoundMorphUpdatePacket pkt, RegistryAccess registries, CompoundTag tag) {
		ClientboundBlockEntityDataPacket.create(blockEntity, (_, _) -> tag).handle(pkt.getListener());
	}

	@Override
	public void sendServer(BlockMorphPacket packet) {
		ClientPlayNetworking.send(new MainPacket(packet));
	}

	@Override
	public void sendPlayer(BlockMorphPacket packet, ServerPlayer player) {
		ServerPlayNetworking.send(player, new MainPacket(packet));
	}

	@Override
	public void sendAll(BlockMorphPacket packet) {
		for (ServerPlayer p : Config.getServer().getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(p, new MainPacket(packet));
		}
	}
}
