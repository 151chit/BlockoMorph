package net.blockomorph.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;

public interface PlayerAccessor {
    void applyBlockMorph(BlockState state, CompoundTag tag, boolean mb);
    void applyBlockMorph(BlockState state, CompoundTag tag);
    InteractionResult clickPlayer(Player clicker, BlockHitResult hiter, InteractionHand hand);
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
    boolean isFullActive();
    PrimedTnt getTnt();
    void setTnt();
}
