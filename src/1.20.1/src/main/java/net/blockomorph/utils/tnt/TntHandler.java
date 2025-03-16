package net.blockomorph.utils.tnt;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TntHandler {
    private final SynchedEntityData entityData;
    private final EntityDataAccessor<CompoundTag> BRAKE_PROGRESS;
    private PrimedTnt tnt;
    private final Player player;
    private final PlayerAccessor pl;

    public TntHandler(PlayerAccessor owner, SynchedEntityData entityData, EntityDataAccessor<CompoundTag> br) {
        this.player = (Player) owner;
        this.pl = owner;
        this.BRAKE_PROGRESS = br;
        this.entityData = entityData;
    }

    public void tick() {
        if (this.tnt != null) {
            try {
                tnt.setPosRaw(this.player.getX(), this.player.getY() + 0.06125D, this.player.getZ());
                tnt.setOldPosAndRot();
                tnt.tick();
                if (!this.player.level().isClientSide) {
                    this.setFuse(tnt.getFuse());
                    if (!tnt.isAlive()) {
                        //tnt = null;
                        MorphUtils.destroy(this.pl, null);
                        tnt = null;
                        this.setFuse(-1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!this.player.level().isClientSide) this.pl.applyBlockMorph(Blocks.AIR.defaultBlockState(), new CompoundTag());
            }
        }
        //if (this.level().getBlockState(this.blockPosition()).getBlock() instanceof BaseFireBlock && !this.level().isClientSide) this.setTnt();
        if (this.player.getRemainingFireTicks() > 0 && !this.player.level().isClientSide) {
            this.setTnt();
        }
    }

    public void setFuse(int i) {
        CompoundTag progress = this.entityData.get(BRAKE_PROGRESS);
        progress = progress.copy();
        progress.putInt("fuse", i);
        this.entityData.set(BRAKE_PROGRESS, progress);
    }

    public void setTnt() {
        BlockState state = this.pl.getBlockState();
        if (state.getBlock() instanceof TntBlock tnt && this.tnt == null) {
            TntSpawnLevel lv = new TntSpawnLevel(this.player.level(), false, state);
            PrimedTnt TNT;
            try {
                tnt.onCaughtFire(state, lv, this.player.blockPosition(), null, this.player);
            } catch (Exception e) {
                TNT = lv.extractTnt();
                if (TNT == null) {
                    e.printStackTrace();
                    return;
                }
            }
            TNT = lv.extractTnt();
            if (TNT == null) {
                BlockPos ps = this.player.blockPosition();
                PrimedTnt primedtnt = new PrimedTnt(this.player.level(), (double)ps.getX() + 0.5D, (double)ps.getY(), (double)ps.getZ() + 0.5D, null);
                this.player.level().playSound((Player)null, primedtnt.getX(), primedtnt.getY(), primedtnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.player.level().gameEvent(null, GameEvent.PRIME_FUSE, ps);
                this.tnt = primedtnt;
            } else
                this.tnt = TNT;
            this.tnt.level();
            this.tnt.setNoGravity(true);
            if (this.player.level().isClientSide()) {
                double d0 = this.player.level().random.nextDouble() * (double)((float)Math.PI * 2F);
                this.player.setDeltaMovement(new Vec3(-Math.sin(d0) * 0.02D, (double)0.2F, -Math.cos(d0) * 0.02D));
            }
        }
    }

    public InteractionResult clckTnt(Player clicker, BlockHitResult hiter, InteractionHand hand) {
        ItemStack itemstack = clicker.getItemInHand(hand);
        if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        } else {
            if (!clicker.level().isClientSide)this.setTnt();
            Item item = itemstack.getItem();
            if (!clicker.isCreative()) {
                if (itemstack.is(Items.FLINT_AND_STEEL)) {
                    itemstack.hurtAndBreak(1, clicker, (pl) -> {
                        pl.broadcastBreakEvent(hand);
                    });
                } else {
                    itemstack.shrink(1);
                }
            }

            clicker.awardStat(Stats.ITEM_USED.get(item));
            return InteractionResult.sidedSuccess(clicker.level().isClientSide);
        }
    }

    public void onClientUpdater() {
        if (this.player.level().isClientSide()) {
            int i = this.entityData.get(BRAKE_PROGRESS).getInt("fuse");
            if (i < 0) {
                this.tnt = null;
            } else {
                if (this.tnt == null) {
                    this.setTnt();
                }
                if (this.tnt != null) {
                    this.tnt.setFuse(i);
                }
            }
        }
    }

    public PrimedTnt getTnt() {
        return this.tnt;
    }

    public void setTnt(PrimedTnt tnt) {
        this.tnt = tnt;
    }
}
