package net.blockomorph.network;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ServerBoundInteractBlockPacket implements BlockMorphPacket {
    public static final String ID = "server_bound_interact_block_packet";
    boolean click;
    int id;
    BlockPos pos;
    public ServerBoundInteractBlockPacket(boolean click, int id, BlockPos pos) {
        this.click = click;
        this.id = id;
        this.pos = pos;
    }

    public ServerBoundInteractBlockPacket(FriendlyByteBuf buffer) {
        this.click = buffer.readBoolean();
        this.id = buffer.readInt();
        this.pos = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.click);
        buffer.writeInt(this.id);
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void handle(Player player) {
        if (pos == null)
            return;
        if (click) {
            Entity ent = ((ServerLevel) player.level()).getEntityOrPart(id);
            if (id < 0 || !(ent instanceof Player))
                return;
            MorphUtils.onPlayerAttack(player, ent, pos);
        } else {
            if (MorphUtils.getEntityLookedAt(player, -1, 1) instanceof PlayerAccessor mob) {
                mob.removePlayer(pos, player);
            }
        }
    }
}
