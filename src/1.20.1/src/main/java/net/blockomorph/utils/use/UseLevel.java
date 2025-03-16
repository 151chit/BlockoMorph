package net.blockomorph.utils.use;

import net.blockomorph.network.blockFix.ClientBoundBlockEventPacket;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.MultiBlockLevel;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UseLevel extends MultiBlockLevel {
    protected final UseController useController;
    private boolean realBlockPosMode;
    private final BlockPos offset;

    public UseLevel(Level lv, boolean client, UseController ctr) {
        super(lv, client);
        this.useController = ctr;
        this.offset = ctr.getOffset();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos bl) {
        return this.useController.getBlockEntity();
    }

    @Override
    public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
        BlockPos pos = this.calculateRealPos(blockPos);
        return super.setBlock(pos, blockState, i, j);
    }

    public void setRealBlockPosMode(boolean b) {
        this.realBlockPosMode = b;
    }

    @Override
    public @NotNull List<? extends Player> players() {
        ArrayList<? extends Player> pls = new ArrayList<>(realLevel.players());
        pls.remove(this.useController.getOwner());
        return pls;
    }

    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        BlockPos pos = this.calculateRealPos(blockPos);
        if (this.realBlockPosMode) {
            if (pos.equals(this.offset)) {
                return this.useController.getBlockState();
            } else {
                return realLevel.getBlockState(blockPos);
            }
        }
        BlockState st = ((PlayerAccessor)this.useController.getOwner()).getBlocks().get(pos);
        if (st == null) return Blocks.AIR.defaultBlockState();
        return st;
    }

    @Override
    public void blockEntityChanged(BlockPos p_151544_) {
        this.useController.checkChanges();
    }

    @Override
    public boolean addFreshEntity(Entity ent) {
        return realLevel.addFreshEntity(ent);
    }

    public void playSound(@Nullable Player pl, BlockPos p_46561_, SoundEvent p_46562_, SoundSource p_46563_, float p_46564_, float p_46565_) {
        Player owner = this.useController.getOwner();
        p_46561_ = this.calculateRealPos(p_46561_);
        realLevel.playSound(pl,
                (double)p_46561_.getX() + 0.5D + owner.getX(),
                (double)p_46561_.getY() + 0.5D + owner.getY(),
                (double)p_46561_.getZ() + 0.5D + owner.getZ(),
                p_46562_, p_46563_, p_46564_, p_46565_);
    }

    public void playLocalSound(BlockPos p_250938_, SoundEvent p_252209_, SoundSource p_249161_, float p_249980_, float p_250277_, boolean p_250151_) {
        Player owner = this.useController.getOwner();
        p_250938_ = this.calculateRealPos(p_250938_);
        realLevel.playLocalSound(
                (double)p_250938_.getX() + 0.5D + owner.getX(),
                (double)p_250938_.getY() + 0.5D + owner.getY(),
                (double)p_250938_.getZ() + 0.5D + owner.getZ(),
                p_252209_, p_249161_, p_249980_, p_250277_, p_250151_);
    }

    public void playSeededSound(@Nullable Player var1, double x, double y, double z, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12) {
        Player owner = this.useController.getOwner();
        if (!this.realBlockPosMode && !(BlockPos.containing(x, y, z).equals(this.useController.getOwner().blockPosition()))) {
            x = x + owner.getX();
            y = y + owner.getY();
            z = z + owner.getZ();
        }
        //System.out.println(new Vec3(x, y, z));
        realLevel.playSeededSound(var1, x, y, z, var8, var9, var10, var11, var12);
    }

    public void blockEvent(BlockPos blockPos, Block block, int a, int b) {
        if (this.useController.getBlockState().triggerEvent(this, this.offset, a, b)) {
            if (!this.realLevel.isClientSide)
                MorphUtils.sendAll(ClientBoundBlockEventPacket.blockEvent(this.useController.getOwner().getId(), this.useController.getOffset(), a, b));
        }
    }

    public void levelEvent(@Nullable Player player, int a, BlockPos blockPos, int b) {
        if (a == 1010) a = -2;
        if (a == 1011) a = -3;
        if (!this.realLevel.isClientSide) {
            ClientBoundBlockEventPacket packet = ClientBoundBlockEventPacket.levelEvent(this.useController.getOwner().getId(), this.useController.getOffset(), a, b);
            if (player == null) {
                MorphUtils.sendAll(packet);
            } else if (player instanceof ServerPlayer pl){
                MorphUtils.sendPlayer(packet, pl);
            }
        } else {

        }
    }

    private BlockPos calculateRealPos(BlockPos blockPos) {
        if (this.realBlockPosMode || blockPos.equals(this.useController.getOwner().blockPosition())) {
            int x = blockPos.getX() - this.useController.getOwner().getBlockX();
            int y = blockPos.getY() - this.useController.getOwner().getBlockY();
            int z = blockPos.getZ() - this.useController.getOwner().getBlockZ();
            return new BlockPos(x, y, z);
        }
        return blockPos;
    }
}
