package net.blockomorph.network;

import net.blockomorph.utils.MultiBlockLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/*********************************************************************************
Temporary package, does not carry any functions, created exclusively for testing!
*********************************************************************************/

public class DebugPacket implements BlockMorphPacket {
    private static final Block bl = Blocks.OAK_SIGN;
    private static final BlockEnt func = new BlockEnt();
    public DebugPacket(BlockHitResult s) {
        this.pos = s;
    }
    public DebugPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockHitResult();
    }

    BlockHitResult pos;

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockHitResult(this.pos   );
    }

    @Override
    public String getId() {
        return "db";
    }

    @Override
    public void handle(Player player) {
        if (!pos.getBlockPos().equals(BlockPos.ZERO)) {
            handleAlternative(this.pos, player);
            return;
        }
        MultiBlockLevel lv = new MultiBlockLevel(player.level(), false) {
        public BlockEntity getBlockEntity(BlockPos blockPos) {
                return func;
            }
        };
        func.setLevel(lv);
        func.setPos(player.blockPosition());
        //((EntityBlock) Blocks.CHEST).getTicker(null, null ,null       );
        bl.defaultBlockState().use(lv, player, InteractionHand.MAIN_HAND, new BlockHitResult(player.position(), Direction.DOWN, player.blockPosition(), false));
    }

    public static void handleAlternative(BlockHitResult pos, Player pl) {
        {
            //BlockState st = pl.level().getBlockState(pos.getBlockPos());
            MultiBlockLevel lv = new MultiBlockLevel(pl.level(), pl.level().isClientSide);

            //st = st.cycle(BlockStateProperties.OPEN);
            //pl.level().setBlock(pos.getBlockPos(), st, 10);
            pl.level().setBlock(pos.getBlockPos().below(), Blocks.AIR.defaultBlockState(), 10   );
            //pl.level().gameEvent(pl, st.getValue(BlockStateProperties.OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos.getBlockPos());
        }
    }



    private static class BlockEnt extends SignBlockEntity {
        BlockPos pos = BlockPos.ZERO;

        public BlockEnt() {
            super(BlockPos.ZERO, bl.defaultBlockState());
        }

        @Override
        public @NotNull BlockPos getBlockPos() {
            return pos;
        }

        public void setPos(BlockPos ps) {
            this.pos = Objects.requireNonNull(ps);
        }
    }
}
