package net.blockomorph.platformUtilsImpl;

import net.blockomorph.Blockomorph;
import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.network.MainPacket;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.blockomorph.utils.tnt.TntSpawnLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public class NeoCommonUtils implements CommonPlatformUtils {

	@Override
	public void litTnt(TntBlock blockVanilla, TntSpawnLevel lv, BlockInPlayer2 block, PlayerAccessor pl) {
		blockVanilla.onCaughtFire(block.getBlockState(), lv, block.getPos(), null, pl.player());
	}

	@Override
	public TntSpawnLevel createLevel(Level orig, BlockState need) {
		return new TntSpawnLevel(orig, need) {};
	}

	@Override
	public void loadNbtToBlockEntityOnClient(BlockEntity blockEntity, ClientBoundMorphUpdatePacket pkt, CompoundTag tag) {
		blockEntity.onDataPacket(pkt.getListener().getConnection(), ClientboundBlockEntityDataPacket.create(blockEntity, ent -> tag));
	}

	@Override
	public void sendServer(BlockMorphPacket packet) {
		Blockomorph.PACKET_HANDLER.sendToServer(new MainPacket(packet));
	}

	@Override
	public void sendPlayer(BlockMorphPacket packet, ServerPlayer player) {
		Blockomorph.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new MainPacket(packet));
	}

	@Override
	public void sendAll(BlockMorphPacket packet) {
		Blockomorph.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new MainPacket(packet));
	}
}
