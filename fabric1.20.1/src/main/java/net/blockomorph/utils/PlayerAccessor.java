package net.blockomorph.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface PlayerAccessor {
    void applyBlockMorph(BlockState state, CompoundTag tag);
    BlockState getBlockState();
    CompoundTag getTag();
    boolean isActive();
    int getProgress();
    void addPlayer(Player player);
    void removePlayer(Player pl);
    VoxelShape getShape();
    VoxelShape getRenderShape();
    boolean readyForDestroy();
    void setReady(boolean flag);
}
