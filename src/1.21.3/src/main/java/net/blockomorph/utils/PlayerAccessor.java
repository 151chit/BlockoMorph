package net.blockomorph.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.HashMap;
import net.minecraft.core.BlockPos;

public interface PlayerAccessor {
    void applyBlockMorph(BlockState state, CompoundTag tag, boolean mb);
    void applyBlockMorph(BlockState state, CompoundTag tag);
    BlockState getBlockState();
    CompoundTag getTag();
    boolean isActive();
    CompoundTag getProgress();
    void addPlayer(BlockPos pos, Player player);
    void removePlayer(BlockPos pos, Player pl);
    VoxelShape getShape();
    VoxelShape getRenderShape(BlockPos pos);
    boolean readyForDestroy();
    void setReady(boolean flag);
    HashMap<BlockPos, BlockState> getBlocks();
    int getBiggestProgress();
    boolean isMultiBlock();
    BlockPos minPos();
}
