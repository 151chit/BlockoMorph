package net.blockomorph.utils;

import net.blockomorph.network.ClientBoundMorphUpdatePacket;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.tnt.TntHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public interface PlayerAccessor {
    MorphUtils.BannedBlock applyBlockMorph(BlockState state, CompoundTag tag);
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
    void setTnt();
    //client only
    //MultiBlockLevel getLiquidCachedLevel();

    default Player player() {
        return (Player) this;
    }
    static PlayerAccessor of(Player pl) {
        return (PlayerAccessor) pl;
    }

    //new
    BlockState getBlockState(InPlayerBlockPos pos);
    boolean setBlockState(InPlayerBlockPos pos, BlockState state, boolean updateInternal);
    BlockEntity getBlockEntity(InPlayerBlockPos pos);
    CompoundTag getTag(InPlayerBlockPos pos);
    void loadBlockData(CompoundTag blockomorph, @Nullable ClientBoundMorphUpdatePacket client);
    HashMap<InPlayerBlockPos, BlockInPlayer2> getBlocksData2();
    CompoundTag saveBlockData(boolean client);
    void prepareSync(InPlayerBlockPos pos);
    void prepareSync(InPlayerBlockPos pos, BlockEventData data);
    void sendNearby(Packet<?> packet);
    boolean isOnLoadingBlocks();
    void breakingModeStart(boolean yes);
    boolean isBreaking();
    List<InPlayerBlockPos> getUpdates();
}
