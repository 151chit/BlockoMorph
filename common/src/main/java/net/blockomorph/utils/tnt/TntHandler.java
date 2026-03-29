package net.blockomorph.utils.tnt;

import net.blockomorph.utils.*;
import net.blockomorph.utils.accessors.ForceLevelChanger;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.dataSyncher.IntSyncedData;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TntHandler {
	private final IntSyncedData BRAKE_PROGRESS;
	private PrimedTnt tnt;
	private final Player player;
	private final PlayerAccessor pl;

	public TntHandler(PlayerAccessor owner) {
		this.player = (Player) owner;
		this.pl = owner;
		this.BRAKE_PROGRESS = new IntSyncedData(owner.player(), MorphUtils.res("tnt_progress"), -1, this::onClientUpdater);
	}

	public void onDimensionChange() {
		Entity tnt1 = tnt.getType().create(this.player.level());
		if (tnt1 instanceof PrimedTnt tnt2 && !this.player.level().isClientSide) {
			tnt2.restoreFrom(this.tnt);
			this.tnt = tnt2;
		}
	}

	public void tick() {
		if (this.tnt != null) {
			if (!this.player.level().isClientSide && this.tnt.level() != this.player.level()) {
				this.onDimensionChange();
				return;
			}
			ProfilerFiller profiler = this.player.level().getProfiler();
			profiler.push("morphedPlayerTntFuseTick");
			try {
				tnt.setPosRaw(this.player.getX(), this.player.getY() + 0.06125D, this.player.getZ());
				tnt.setOldPosAndRot();
				tnt.tick();
				if (!this.player.level().isClientSide) {
					this.setFuse(tnt.getFuse());
					if (!tnt.isAlive()) {
						if (Config.get().playerDieAfterDestroy.getValue()) {
							DamageHandler.destroy(this.pl, null);
						} else {
							this.deMorph();
						}
						tnt = null;
						this.setFuse(-1);
					}
				}
			} catch (Exception e) {
				MorphUtils.LOGGER.error("Error while ticking TNT in morphed player " + this.player.getDisplayName().getString(), e);
				this.deMorph();
			} finally {
				profiler.pop();
			}
		}
		if (this.player.getRemainingFireTicks() > 0 && !this.player.level().isClientSide) {
			this.runTnt();
		}
	}

	private void deMorph() {
		if (!this.player.level().isClientSide)
			this.pl.applyBlockMorph(Blocks.AIR.defaultBlockState(), null, BannedBlock.Source.SYSTEM);
	}

	public void setFuse(int i) {
		BRAKE_PROGRESS.set(i);
	}

	public boolean runTnt() {
		if (this.pl.getBlocksData2().size() == 1 && this.pl.getBlocksData2().containsKey(InPlayerBlockPos.ZERO)) {
			BlockInPlayer2 block = this.pl.getBlocksData2().get(InPlayerBlockPos.ZERO);
			if (block != null) {
				BlockState state = block.getBlockState();
				if (state != null && state.getBlock() instanceof TntBlock tntBlock && this.tnt == null) {
					TntSpawnLevel lv = TntSpawnLevel.create(this.player.level(), state);
					PrimedTnt TNT;
					try {
						CommonPlatformUtils.INSTANCE.litTnt(tntBlock, lv, block, this.pl);
					} catch (Exception e) {
						TNT = lv.extractTnt();
						if (TNT == null) {
							MorphUtils.LOGGER.error("Error while init TNT in morphed player " + this.player.getDisplayName().getString(), e);
							return false;
						}
					}
					TNT = lv.extractTnt();
					Vec3 ps = MorphUtils.getRealBlockPos(this.pl, block.getOffset());
					if (TNT == null) {
						PrimedTnt primedtnt = new PrimedTnt(this.player.level(), ps.x + 0.5D, ps.y, ps.z + 0.5D, null);
						this.player.level().playSound(null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
						this.player.level().gameEvent(null, GameEvent.PRIME_FUSE, ps);
						this.tnt = primedtnt;
					} else {
						TNT.setPos(ps);
						this.tnt = TNT;
					}
					ForceLevelChanger.of(this.tnt).forceLevelChange(this.player.level());
					this.tnt.setNoGravity(true);
					if (this.player.level().isClientSide()) {
						double d0 = this.player.level().random.nextDouble() * (double) ((float) Math.PI * 2F);
						this.player.addDeltaMovement(new Vec3(-Math.sin(d0) * 0.02D, 0.3F, -Math.cos(d0) * 0.02D));
					}
					return true;
				}
			}
		}
		return false;
	}

	@Nullable
	public InteractionResult clickTnt(Player clicker, InteractionHand hand, InPlayerBlockPos realPos) {
		BlockState state = pl.getBlockState(realPos);
		if (realPos.equals(InPlayerBlockPos.ZERO) && state != null && state.getBlock() instanceof TntBlock) {
			ItemStack itemstack = clicker.getItemInHand(hand);
			if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
				return null;
			} else {
				if (!clicker.level().isClientSide) {
					if (!this.runTnt()) {
						return InteractionResult.FAIL;
					}
				}
				Item item = itemstack.getItem();
				if (!clicker.isCreative()) {
					if (itemstack.is(Items.FLINT_AND_STEEL)) {
						itemstack.hurtAndBreak(1, clicker, (pla) -> {
							pla.broadcastBreakEvent(hand);
						});
					} else {
						itemstack.shrink(1);
					}
				}

				clicker.awardStat(Stats.ITEM_USED.get(item));
				return InteractionResult.sidedSuccess(clicker.level().isClientSide);
			}
		}
		return null;
	}

	public void onClientUpdater() {
		if (this.player.level().isClientSide()) {
			int i = BRAKE_PROGRESS.get();
			if (i < 0) {
				this.tnt = null;
			} else {
				if (this.tnt == null) {
					this.runTnt();
				}
				if (this.tnt != null) {
					this.tnt.setFuse(i);
				}
			}
		}
	}

	public void stopRunning() {
		this.tnt = null;
		this.setFuse(-1);
	}

	public PrimedTnt getTnt() {
		return this.tnt;
	}
}
