package net.blockomorph.utils;

import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.gameEvent.PlayerDynamicGameEventListener;
import net.blockomorph.utils.tick.InPlayerBlockEntityTickManager;
import net.blockomorph.utils.tnt.TntHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface PlayerAccessor {
	BannedBlock applyBlockMorph(BlockState state, CompoundTag tag, BannedBlock.Source source);

	void sendAllContentToPlayer(ServerPlayer player);

	boolean isActive();

	boolean isFullActive();

	VoxelShape getShape(InPlayerBlockPos offset, @Nullable Vec3 realPos);

	VoxelShape getRenderShape(InPlayerBlockPos pos, Player colliser);

	int getBiggestProgress();

	InPlayerBlockPos minPos();

	InPlayerBlockPos maxPos();

	PrimedTnt getTnt();

	TntHandler getTntHandler();

	HitBoxCalculator getHitBoxHandler();

	PlayerDynamicGameEventListener getListenersStorage();

	InPlayerBlockEntityTickManager getBlockEntityTickManager();

	void setTnt();

	BlockState getBlockState(InPlayerBlockPos pos);

	boolean setBlockState(InPlayerBlockPos pos, BlockState state, int flags);

	BlockEntity getBlockEntity(InPlayerBlockPos pos);

	CompoundTag getTag(InPlayerBlockPos pos);

	void loadBlockData(CompoundTag blockomorph, @Nullable ClientBoundMorphUpdatePacket client, boolean first);

	Map<InPlayerBlockPos, BlockInPlayer2> getBlocksData2();

	void getBlocksData2InArea(AABB box, TriConsumer<InPlayerBlockPos, BlockInPlayer2, Vec3> action);

	CompoundTag saveBlockData(boolean client);

	void prepareSync(InPlayerBlockPos pos);

	@ApiStatus.Internal
	void prepareSync(InPlayerBlockEventData data);

	@ApiStatus.Internal
	record InPlayerBlockEventData(InPlayerBlockPos inPlayerPosOfBlock, BlockEventData data) {
		@Override
		public int hashCode() {
			return this.data.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof InPlayerBlockEventData dat) {
				return dat.data.equals(this.data);
			}
			return false;
		}
	}

	void sendNearby(Packet<? super ClientGamePacketListener> packet);

	boolean isOnLoadingBlocks();

	void setOnLoadingBlocks(boolean yes);

	void breakingModeStart(boolean yes);

	boolean isBreaking();

	boolean isUnContextedBreaking();

	Set<InPlayerBlockPos> getUpdates();


	default Player player() {
		return (Player) this;
	}

	static PlayerAccessor of(Player pl) {
		return (PlayerAccessor) pl;
	}
}
