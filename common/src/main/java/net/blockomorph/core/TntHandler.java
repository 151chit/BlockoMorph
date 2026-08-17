package net.blockomorph.core;

import net.blockomorph.network.ClientBoundSyncTntFusePacket;
import net.blockomorph.network.ClientBoundSyncTntNbtPacket;
import net.blockomorph.core.misc.DamageHandler;
import net.blockomorph.core.serialization.io.BlockEntityAndEntityIO;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.side.Side;
import net.blockomorph.utils.accessors.Accessors;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.blockomorph.utils.config.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class TntHandler {
	private final BlockEntityAndEntityIO ioWorker = new BlockEntityAndEntityIO();
	private static UUID TICKING_THIS_TICK;
	private final InPlayerManager manager;
	private PrimedTnt primedTnt;
	private PrimedTnt delayedTnt;

	protected TntHandler(InPlayerManager manager) {
		this.manager = manager.assertOnInit();
	}

	public PrimedTnt getActiveTnt() {
		return this.primedTnt;
	}

	public boolean isActive() {
		return this.getActiveTnt() != null;
	}

	public static UUID getTickingThisTick() {
		if (TICKING_THIS_TICK != null && Side.get() == Side.SERVER) {
			return TICKING_THIS_TICK;
		}
		return null;
	}

	private Player player() {
		return this.manager.getOwner().player();
	}

	protected void tick() {
		if (this.delayedTnt != null) {
			this.putTntFromLevel(this.delayedTnt);
			this.delayedTnt = null;
		}
		if (this.isActive()) {
			if (this.isValidForEnterTntMode(this.primedTnt)) try {
				this.syncTntEntityForPlayerEntity();
				this.tickWithDisabledLevelAdding();
				if (this.manager.isServer()) {
					this.setFuse(this.primedTnt.getFuse());
					if (this.primedTnt.isRemoved()) {
						if (this.primedTnt.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION)
							this.changeLevel(this.manager.level());
						else {
							this.stop();
							if (Config.get().dieAfterTntExplode.getValue()) {
								DamageHandler.destroy(this.manager.getOwner(), null);
							}
						}
					}
				}
			} catch (Exception e) {
				MorphUtils.LOGGER.error("Error while ticking tnt in playerOwner: {}", this.manager, e);
				this.stop();
			} else {
				this.stop();
			}
		}
	}

	private void tickWithDisabledLevelAdding() {
		TICKING_THIS_TICK = this.primedTnt.getUUID();
		try {
			this.primedTnt.tick();
		} finally {
			TICKING_THIS_TICK = null;
		}
	}

	public void tryActivateDirect() {
		if (!this.isActive() && this.manager.isServer() && this.manager.getBlocksStorage().size() == 1) {
			BlockInPlayer2 block = this.manager.getBlocksStorage().randomSortedBlockOrThrow();
			if (block.getBlockState().getBlock() instanceof TntBlock tntBlock) {
				LevelWithFlags.of(this.manager.level()).flags().redstoneAlwaysOn = true;
				try {
					block.getBlockState().handleNeighborChanged(this.manager.level(), block.getPos(), Blocks.REDSTONE_BLOCK, null, false);
				} catch (Exception e) {
					MorphUtils.LOGGER.error("Error while trying load tnt direct in playerOwner: {} for tnt: {}", this.manager, tntBlock, e);
				} finally {
					LevelWithFlags.of(this.manager.level()).flags().redstoneAlwaysOn = false;
				}
			}
		}
	}

	public void syncForNetwork(ServerPlayer client) {
		if (this.isActive() && this.manager.isServer()) {
			client.connection.send(new ClientboundAddEntityPacket(this.primedTnt, 0, this.manager.getZeroKey()));
			client.connection.send(new ClientBoundSyncTntNbtPacket(this.saveFullTag(), this.manager).toVanillaClientbound());
		}
	}

	public void stop() {
		this.primedTnt = null;
		if (this.manager.isServer())
			this.send(new ClientBoundSyncTntFusePacket(-1, this.manager).toVanillaClientbound());
	}

	public void putTntDirectFromDisk(PrimedTnt tnt) {
		this.manager.assertOnDataSerialization();
		this.delayedTnt = tnt;
	}

	public boolean putTntFromLevel(PrimedTnt tnt) {
		if (this.isActive()) this.stop();
		if (!this.isValidForEnterTntMode(tnt)) return false;
		this.primedTnt = tnt;
		this.primedTnt.discard();
		Accessors.EntityAccessor.of(this.primedTnt).unmarkRemoved$bm();
		this.primedTnt.setLevelCallback(EntityInLevelCallback.NULL);
		if (this.manager.isServer()) {
			this.send(new ClientboundAddEntityPacket(this.primedTnt, 0, this.manager.getZeroKey()));
			this.send(new ClientBoundSyncTntNbtPacket(this.saveFullTag(), this.manager).toVanillaClientbound());
		}
		this.syncTntEntityForPlayerEntity();
		if (this.manager.isServer()) {
			double d0 = this.manager.level().getRandom().nextDouble() * (double) ((float) Math.PI * 2F);
			Vec3 movement = this.player().getDeltaMovement().add(new Vec3(-Math.sin(d0) * 0.02D, 0.3F, -Math.cos(d0) * 0.02D));
			this.send(new ClientboundSetEntityMotionPacket(this.player().getId(), movement));
		}
		return true;
	}

	public void setFuse(int time) {
		if (time < 0) this.stop();
		else if (this.isActive()) {
			this.primedTnt.setFuse(time);
			if (this.manager.isServer()) {
				this.send(new ClientBoundSyncTntFusePacket(time, this.manager).toVanillaClientbound());
			}
		}
	}

	public void loadFullTag(CompoundTag tg) {
		if (this.isActive()) {
			var result = this.ioWorker.loadInEntity(this.primedTnt, this.manager.level().registryAccess(), tg);
			BlockEntityAndEntityIO.log(result, "load tnt data for playerOwner " + this.player());
			if (this.manager.isServer()) {
				this.send(new ClientBoundSyncTntNbtPacket(tg, this.manager).toVanillaClientbound());
			}
		}
	}

	private boolean isValidForEnterTntMode(PrimedTnt tnt) {
		if (tnt != null && this.manager.getBlocksStorage().size() == 1) {
			return this.manager.getBlocksStorage().randomSortedBlockOrThrow().getBlockState().getBlock() instanceof TntBlock;
		}
		return false;
	}

	protected void changeLevel(Level lv) {
		if (this.isActive() && this.manager.isServer()) {
			Entity entity = this.primedTnt.getType().create(lv, EntitySpawnReason.DIMENSION_TRAVEL);
			if (entity instanceof PrimedTnt tnt) {
				tnt.restoreFrom(this.primedTnt);
				this.primedTnt = tnt;
				this.syncTntEntityForPlayerEntity();
			}
		}
	}

	private void send(Packet<? super ClientGamePacketListener> pkt) {
		if (this.manager.level() instanceof ServerLevel lv) {
			lv.getChunkSource().broadcastAndSend(this.player(), pkt);
		}
	}

	private void syncTntEntityForPlayerEntity() {
		this.primedTnt.snapTo(this.player().getX(), this.player().getY() + 0.06125, this.player().getZ());
		this.primedTnt.setOnGround(this.player().onGround());
	}

	private CompoundTag saveFullTag() {
		var result = this.ioWorker.saveEntity(this.primedTnt, this.manager.level().registryAccess());
		BlockEntityAndEntityIO.log(result.errors(), "save tnt data for playerOwner " + this.player());
		return result.result();
	}
}
