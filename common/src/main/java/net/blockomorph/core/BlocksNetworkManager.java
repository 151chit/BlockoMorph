package net.blockomorph.core;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.serialization.BlockPalette;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlocksNetworkManager {
	private final IntSet networkUpdates = new IntOpenHashSet(BlocksInPlayerStorage.ONE_AXIS);
	private final List<BlockInPlayer2> blockEntitiesForSent = new ArrayList<>(BlocksInPlayerStorage.ONE_AXIS);
	private final List<BlockInPlayer2> blockEntitiesForSentImmediate = new ArrayList<>(BlocksInPlayerStorage.ONE_AXIS);
	private final ObjectLinkedOpenHashSet<BlockEventData> eventUpdates = new ObjectLinkedOpenHashSet<>();
	private final InPlayerManager manager;
	private final int heavyMode;

	protected BlocksNetworkManager(InPlayerManager manager, int heavyMode) {
		this.manager = manager.assertOnInit();
		this.heavyMode = heavyMode;
	}

	public void sendAllBlocksImmediateToPlayer(ServerPlayer player) {
		if (this.manager.isServer()) try {
			BlockPalette palette = BlockPalette.packAll(this.manager.getBlocksStorage(), block -> {
				if (block.getBlockEntity() != null) {
					this.blockEntitiesForSentImmediate.add(block);
				}
			});
			player.connection.send(palette.toPacket(this.manager.getOwner()).toVanillaClientbound());
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < this.blockEntitiesForSentImmediate.size(); i++) {
				this.sendBlockEntityData(this.blockEntitiesForSentImmediate.get(i), player);
			}
		} finally {
			this.blockEntitiesForSentImmediate.clear();
		}
	}

	public void enqueueBlockNetworkUpdate(int inPlayerBlockPos) {
		if (this.manager.isServer()) this.networkUpdates.add(inPlayerBlockPos);
	}

	public void enqueueBlockEventData(BlockEventData data) {
		if (InPlayerBlockPos.isInvalidPosFor(this.manager, data.pos())) return;
		if (this.manager.isServer()) this.eventUpdates.add(data);
	}

	private Level level() {
		return this.manager.level();
	}

	protected void tick() {
		if (this.manager.isServer()) {
			this.runNetworkUpdates();
			this.runBlockEventsUpdates();
		}
	}

	protected void runNetworkUpdates() {
		if (this.networkUpdates.isEmpty()) return;
		if (this.networkUpdates.size() > this.heavyMode) {
			this.runHeavyNetworkUpdate();
		} else this.runLiteNetworkUpdate();
		this.networkUpdates.clear();
	}

	private void runLiteNetworkUpdate() {
		this.networkUpdates.forEach((pkgPos) -> {
			BlockInPlayer2 block = this.manager.getBlocksStorage().get(pkgPos);
			if (block == null) {
				InPlayerBlockPos pos = InPlayerBlockPos.decode(pkgPos);
				if (pos != null) {
					this.sendNearby(new ClientboundBlockUpdatePacket(pos.encodeByManager(this.manager), Blocks.AIR.defaultBlockState()));
				}
			} else {
				this.sendNearby(new ClientboundBlockUpdatePacket(block.getPos(), block.getBlockState()));
				this.sendBlockEntityData(block, null);
			}
		});
	}

	private void runHeavyNetworkUpdate() {
		try {
			BlockPalette palette = BlockPalette.packWithFilter(this.manager.getBlocksStorage(), this.networkUpdates, ignored -> Blocks.AIR.defaultBlockState(), block -> {
				if (block.getBlockEntity() != null) {
					this.blockEntitiesForSent.add(block);
				}
			});
			var packet = palette.toPacket(this.manager.getOwner());
			this.sendNearby(packet.toVanillaClientbound());
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < this.blockEntitiesForSent.size(); i++) {
				this.sendBlockEntityData(this.blockEntitiesForSent.get(i), null);
			}
		} finally {
			this.blockEntitiesForSent.clear();
		}
	}

	private void runBlockEventsUpdates() {
		if (this.eventUpdates.isEmpty()) return;
		while (!this.eventUpdates.isEmpty()) {
			BlockEventData blocksData = this.eventUpdates.removeFirst();
			BlockState block = this.manager.getOwner().getBlockState(InPlayerBlockPos.fromDelta(this.manager.getZeroKey(), blocksData.pos()));
			if (block.is(blocksData.block())) {
				boolean result = block.triggerEvent(this.level(), blocksData.pos(), blocksData.paramA(), blocksData.paramB());
				if (result) this.sendNearby(new ClientboundBlockEventPacket(blocksData.pos(), blocksData.block(), blocksData.paramA(), blocksData.paramB()));
			}
		}
	}

	private void sendNearby(Packet<? super ClientGamePacketListener> packet) {
		if (this.level() instanceof ServerLevel lv) {
			lv.getChunkSource().sendToTrackingPlayersAndSelf(this.manager.getOwner().player(), packet);
		}
	}

	private void sendBlockEntityData(BlockInPlayer2 block, @Nullable ServerPlayer player) {
		BlockEntity ent = block.getBlockEntity();
		if (ent != null) try {
			var packet = ent.getUpdatePacket();
			if (packet != null)
				if (player == null) {
					this.sendNearby(packet);
				} else {
					player.connection.send(packet);
				}
		} catch (Exception e) {
			MorphUtils.LOGGER.error("An error occurred while sending morphed playerOwner data on pos: {} for block: {} on playerOwner: {}", block.getOffset(), block.getBlockState(), this.manager, e);
		}
	}
}
