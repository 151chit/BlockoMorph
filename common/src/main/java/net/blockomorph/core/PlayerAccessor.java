package net.blockomorph.core;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.tick.PlayerTicks;
import net.blockomorph.utils.BannedBlock;
import net.blockomorph.core.misc.DamageHandler;
import net.blockomorph.utils.config.Config;
import net.blockomorph.core.storage.playerSection.PlayerSectionHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public interface PlayerAccessor extends PlayerBlocksEditor {

	void changeManager(InPlayerManager manager);

	default BlockInPlayer2 getBlock(InPlayerBlockPos pos) {
		return this.getBlock(pos.asInt());
	}

	default BlockState getBlockState(InPlayerBlockPos pos) {
		return this.getBlockState(pos.asInt());
	}

	default boolean setBlock(InPlayerBlockPos pos, BlockState state, int flags) {
		return this.setBlock(pos.asInt(), state, flags);
	}

	@Nullable
	default BlockEntity getBlockEntity(InPlayerBlockPos pos) {
		return this.getBlockEntity(pos.asInt());
	}

	@Nullable
	default CompoundTag getTag(InPlayerBlockPos pos) {
		BlockEntity blockEntity = this.getBlockEntity(pos);
		if (blockEntity != null) {
			return blockEntity.saveWithoutMetadata(this.getManager().level().registryAccess());
		}
		return null;
	}

	default BannedBlock applyBlockMorph(BlockState state, CompoundTag tag, BannedBlock.Source source) {
		if (this.isNotInitialized()) throw new IllegalStateException("Try morph in playerOwner constructor!");
		return this.getManager().applyBlockMorph(state, tag, source);
	}

	default boolean isSingleMorph(Block blockType) {
		if (this.size() != 1) return false;
		return this.getBlocksStorage().randomSortedBlockOrThrow().getBlockState().is(blockType);
	}

	default InPlayerBlockPos minPos() {
		if (this.isNotInitialized()) return InPlayerBlockPos.ZERO;
		return this.getManager().getHitBoxCalculator().getMinPos();
	}

	default InPlayerBlockPos maxPos() {
		if (this.isNotInitialized()) return InPlayerBlockPos.ONE;
		return this.getManager().getHitBoxCalculator().getMaxPos();
	}

	default boolean isBlockomorphActive() {
		if (this.isNotInitialized()) return false;
		return this.size() != 0;
	}

	default boolean isBlockomorphFullActive() {
		return this.isBlockomorphActive() && this.getManager().getTntHandler().getActiveTnt() == null;
	}

	default PrimedTnt getActiveMorphTnt() {
		if (this.isNotInitialized()) return null;
		return this.getManager().getTntHandler().getActiveTnt();
	}

	default void sendAllContentToPlayer(ServerPlayer player) {
		if (this.isNotInitialized()) return;
		this.getManager().getNetworkManager().sendAllBlocksImmediateToPlayer(player);
		this.getManager().getTntHandler().syncForNetwork(player);
	}

	default GameEventListenerRegistry getEventListenersStorage() {
		if (this.isNotInitialized()) return GameEventListenerRegistry.NOOP;
		return this.getManager().getEventListenersStorage();
	}

	@SuppressWarnings("unchecked")
	default <T> PlayerTicks<T> getTicksForElementType(T type) {
		if (this.isNotInitialized()) return null;
		if (type instanceof Block) return (PlayerTicks<T>) this.getManager().getBlockTicks();
		if (type instanceof Fluid) return (PlayerTicks<T>) this.getManager().getFluidTicks();
		throw new IncompatibleClassChangeError("Unknown element: " + type);
	}

	default PlayerSectionHandler getSectionHandler() {
		if (this.isNotInitialized()) return null;
		return this.getManager().getSectionHandler();
	}

	default void markBreakingStart(boolean yes) {
		if (this.isNotInitialized()) return;
		this.getManager().getFlags().isBreaking.setValue(yes);
	}

	default void checkDeathNeedWithAttacker(Entity attacker) {
		if (this.isNotInitialized()) return;
		boolean flag = this.getManager().getFlags().isBreaking.isTrue();
		this.markBreakingStart(false);
		if (Config.get().playerDieAfterDestroy.getValue() && flag && this.isSingleMorph(Blocks.VOID_AIR)) {
			this.getManager().getFlags().killedByPlayerHand.setValue(true);
			DamageHandler.destroy(this, attacker);
		}
	}

	default boolean checkAndResetIfPlayerBroken() {
		if (this.isNotInitialized()) return false;
		PlayerFlagSet.Flag flag = this.getManager().getFlags().killedByPlayerHand;
		boolean value = flag.isTrue();
		flag.setValue(false);
		return value;
	}

	static PlayerAccessor of(Player pl) {
		return (PlayerAccessor) pl;
	}
	default Player player() {
		return (Player) this;
	}
}
