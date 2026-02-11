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
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;

public class FabricCommonUtils implements CommonPlatformUtils {

	@Override
	public void litTnt(TntBlock blockVanilla, TntSpawnLevel lv, BlockInPlayer2 block, PlayerAccessor pl) {
		blockVanilla.neighborChanged(block.getBlockState(), lv, block.getPos(), Blocks.REDSTONE_BLOCK, BlockPos.ZERO, false);
	}

	@Override
	public TntSpawnLevel createLevel(Level orig, BlockState need) {
		return new TntSpawnLevel(orig, need) {};
	}

	@Override
	public void loadNbtToBlockEntityOnClient(BlockEntity blockEntity, ClientBoundMorphUpdatePacket pkt, CompoundTag tag) {
		ClientboundBlockEntityDataPacket.create(blockEntity, ent -> tag).handle(pkt.getListener());
	}

	@Override
	public void sendServer(BlockMorphPacket packet) {
		this.wrapPacket(new MainPacket(packet), ClientPlayNetworking::send);
	}

	@Override
	public void sendPlayer(BlockMorphPacket packet, ServerPlayer player) {
		this.wrapPacket(new MainPacket(packet), (res, msg) -> {
			ServerPlayNetworking.send(player, res, msg);
		});
	}

	@Override
	public void sendAll(BlockMorphPacket packet) {
		for (ServerPlayer p : Config.getServer().getPlayerList().getPlayers()) {
			this.wrapPacket(new MainPacket(packet), (res, msg) -> {
				ServerPlayNetworking.send(p, res, msg);
			});
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void wrapPacket(T type, BiConsumer<ResourceLocation, FriendlyByteBuf> sender) {
		FabricRegisterUtils.PacketForRegister<T> register = (FabricRegisterUtils.PacketForRegister<T>) FabricRegisterUtils.PACKETS.get(type.getClass());
		if (register != null) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			register.codec().encode(buf, type);
			sender.accept(register.type(), buf);
			return;
		}
		throw new IllegalArgumentException("Packet not registered: " + type);
	}
}
