package net.blockomorph.network;

import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.SavedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;

/************************************************************************************************************************
 Temporary package, does not carry any functions, created exclusively for testing, it will be removed in the next update!
 ***********************************************************************************************************************/

public class DebugPacket2 implements BlockMorphPacket {
    public DebugPacket2() {}
    public DebugPacket2(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void write(FriendlyByteBuf buffer) {

    }

    @Override
    public String getId() {
        return "db2";
    }

    @Override
    public void handle(Player player) {
        HashMap<BlockPos, SavedBlock> blocks = new HashMap<>();
        blocks.put(new BlockPos(0, 1, 0), new SavedBlock(Blocks.COBBLESTONE.defaultBlockState(), new CompoundTag(), ""));
        blocks.put(new BlockPos(0, 2, 0), new SavedBlock(Blocks.TORCH.defaultBlockState(), new CompoundTag(), ""));
        blocks.put(new BlockPos(1, 0, 0), new SavedBlock(Blocks.CHEST.defaultBlockState(), new CompoundTag(), ""));
        blocks.put(BlockPos.ZERO, new SavedBlock(Blocks.COBBLESTONE.defaultBlockState(), new CompoundTag(), ""));
        ((PlayerAccessor)player).enableBlockOverrides(blocks);
    }
}
