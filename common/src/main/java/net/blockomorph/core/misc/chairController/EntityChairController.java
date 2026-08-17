package net.blockomorph.core.misc.chairController;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.network.ClientBoundChairConnectPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class EntityChairController {
	private final LivingEntity owner;
	private Vec3 inPlayerTargetOffset;
	private UUID targetId;
	private Player targetPlayer;
	private @Nullable BlockCondition targetBlock;
	private DismountFunction dismountServerSuggest;

	public EntityChairController(LivingEntity entity) {
		this.owner = entity;
	}

	public void boundForPlayer(UUID id, Vec3 offset, @Nullable BlockCondition targetBlock, @Nullable DismountFunction dismountServerSuggest) {
		if (InPlayerBlockPos.isByteBoundInvalid(Mth.floor(offset.x), Mth.floor(offset.y), Mth.floor(offset.z))) return;
		if (this.owner.isShiftKeyDown()) return;
		this.targetId = Objects.requireNonNull(id);
		if (this.registerForPlayer() && this.checkValid(true)) {
			this.inPlayerTargetOffset = Objects.requireNonNull(offset);
			this.targetBlock = targetBlock;
			if (this.owner.level() instanceof ServerLevel lv) {
				this.dismountServerSuggest = dismountServerSuggest;
				lv.getChunkSource().broadcastAndSend(this.owner, this.getSyncPacket());
			}
		}
	}

	public LivingEntity getOwner() {
		return this.owner;
	}

	public boolean isActive() {
		return this.targetId != null;
	}

	public Packet<? super ClientGamePacketListener> getSyncPacket() {
		if (!this.isActive()) return null;
		return ClientBoundChairConnectPacket.bound(this.owner, this.targetId, this.inPlayerTargetOffset, this.targetBlock).toVanillaClientbound();
	}

	private boolean registerForPlayer() {
		Player playerCandidate = this.owner.level().getPlayerByUUID(this.targetId);
		if (playerCandidate != null && !playerCandidate.isRemoved()) {
			this.targetPlayer = playerCandidate;
			if (this.targetPlayer instanceof ChairPlayer pl && this.owner instanceof ChairedEntity ent) {
				pl.getHolder().passengers.add(ent);
			}
			return true;
		}
		this.unbound();
		return false;
	}

	public void unbound() {
		if (this.isActive()) {
			if (this.targetPlayer instanceof ChairPlayer pl && this.owner instanceof ChairedEntity entity)
				pl.getHolder().passengers.remove(entity);
			if (this.owner.level() instanceof ServerLevel lv)
				lv.getChunkSource().broadcastAndSend(this.owner, ClientBoundChairConnectPacket.unbound(this.owner).toVanillaClientbound());
		}
		this.targetId = null;
		this.inPlayerTargetOffset = null;
		this.targetPlayer = null;
		this.targetBlock = null;
		this.dismountServerSuggest = null;
	}

	public void rideTick(boolean priorityQueue) {
		if (this.isActive()) {
			if (this.targetPlayer.isRemoved() && !this.registerForPlayer()) return;
			if (!this.checkValid(false)) return;
			if (!priorityQueue && !this.owner.level().isClientSide() && this.owner.isShiftKeyDown()) {
				this.unboundWithTeleport(this::unbound);
				return;
			}
			if (!this.shouldTick(priorityQueue)) return;
			Vec3 realPos = MorphMath.getRealBlockPos(PlayerAccessor.of(this.targetPlayer), this.inPlayerTargetOffset);
			this.owner.setPos(realPos);
			this.owner.setDeltaMovement(Vec3.ZERO);
			if (this.owner instanceof ServerPlayer pl) {
				pl.connection.resetPosition();
			}
		}
	}

	private void unboundWithTeleport(Runnable unbound) {
		Vec3 pos = null;
		if (this.dismountServerSuggest != null) {
			pos = this.dismountServerSuggest.dismountPos(PlayerAccessor.of(this.targetPlayer), this);
		}
		unbound.run();
		if (pos != null) {
			this.owner.teleportTo(pos.x, pos.y, pos.z);
		}
	}

	private boolean checkValid(boolean overridePassenger) {
		if (this.targetPlayer == null) return false;
		if (this.owner.isPassenger()) {
			if (overridePassenger) {
				this.owner.stopRiding();
			} else {
				this.unbound();
				return false;
			}
		}
		if (this.targetBlock != null) {
			BlockState blockState = PlayerAccessor.of(this.targetPlayer).getBlockState(this.targetBlock.blockPos());
			if (!blockState.is(this.targetBlock.block())) {
				this.unbound();
				return false;
			}
		}
		if (this.targetPlayer instanceof ServerPlayer pl) {
			if (pl.isSpectator() || this.owner.isSpectator() ||
					!this.owner.broadcastToPlayer(pl) || (this.owner instanceof ServerPlayer own && !pl.broadcastToPlayer(own))) {
				this.unbound();
				return false;
			}
		}
		return true;
	}

	private boolean shouldTick(boolean priorityQueue) {
		if (!this.owner.level().isClientSide() || this.owner instanceof Player pl && pl.isLocalPlayer()) {
			return priorityQueue;
		}
		return !priorityQueue;
	}

	@FunctionalInterface
	public interface ChairedEntity {
		EntityChairController getController();
	}

	@FunctionalInterface
	public interface ChairPlayer {
		ChairHolder getHolder();
	}

	@FunctionalInterface
	public interface DismountFunction {
		Vec3 dismountPos(PlayerAccessor chair, EntityChairController cntr);
	}

	public record BlockCondition(InPlayerBlockPos blockPos, Block block) {
		public BlockCondition {
			Objects.requireNonNull(blockPos);
			Objects.requireNonNull(block);
		}
	}

	public static class ChairHolder {
		private final Player player;
		private final Set<ChairedEntity> passengers = new ObjectOpenHashSet<>();

		public ChairHolder(Player player) {
			this.player = player;
		}

		public void tick() {
			this.passengers.removeIf(ch ->
				ch.getController().owner.isRemoved() || !this.player.getUUID().equals(ch.getController().targetId)
			);
		}

		public void forEachPassengers(Consumer<ChairedEntity> handler) {
			this.passengers.forEach(handler);
		}
	}
}
