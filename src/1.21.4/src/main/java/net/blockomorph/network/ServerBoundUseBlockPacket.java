package net.blockomorph.network;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class ServerBoundUseBlockPacket implements BlockMorphPacket {
    public static final String ID = "server_bound_use_block_packet";
    BlockHitResult hit;
    InteractionHand hand;
    public ServerBoundUseBlockPacket(BlockHitResult bl, InteractionHand h) {
        this.hit = bl;
        this.hand = h;
    }

    public ServerBoundUseBlockPacket(FriendlyByteBuf buffer) {
        this.hit = buffer.readBlockHitResult();
        this.hand = buffer.readEnum(InteractionHand.class);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockHitResult(this.hit);
        buffer.writeEnum(this.hand);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void handle(Player player) {
        if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor pl) {
            if (pl.clickPlayer(player, hit, hand) instanceof InteractionResult.Success s && s.swingSource() == InteractionResult.SwingSource.SERVER) player.swing(hand, true);
        }
    }
}
