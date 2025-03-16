package net.blockomorph.utils;

import net.blockomorph.utils.use.UseController;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BlockInPlayer {
    private final BlockState blockState;
    private final BlockBracker blockBracker;
    private final UseController useController;

    public BlockInPlayer(BlockState state, BlockBracker bracker, UseController controller) {
        this.blockState = state;
        this.blockBracker = bracker;
        this.useController = controller;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    public BlockBracker getBlockBracker() {
        return this.blockBracker;
    }

    public UseController getUseController() {
        return this.useController;
    }

    public void tick() {
        this.blockBracker.tick();
        this.useController.tick();
    }

}
