package net.blockomorph.network.blockFix;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.use.UseController;
import net.blockomorph.utils.use.UseLevel;
import net.blockomorph.utils.use.fix.PlayerJukeboxSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class ClientBoundBlockEventPacket implements BlockMorphPacket {
    public static final String ID = "client_bound_block_event_packet";
    int playerId;
    BlockPos offset;
    int paramA;
    int paramB;
    boolean levelEvent;

    private ClientBoundBlockEventPacket(boolean levelEvent, int playerId, BlockPos offset, int paramA, int paramB) {
        this.playerId = playerId;
        this.offset = offset;
        this.paramA = paramA;
        this.paramB = paramB;
        this.levelEvent = levelEvent;
    }

    public static ClientBoundBlockEventPacket blockEvent(int playerId, BlockPos offset, int paramA, int paramB) {
        return new ClientBoundBlockEventPacket(false, playerId, offset, paramA, paramB);
    }

    public static ClientBoundBlockEventPacket levelEvent(int playerId, BlockPos offset, int paramA, int paramB) {
        return new ClientBoundBlockEventPacket(true, playerId, offset, paramA, paramB);
    }

    public ClientBoundBlockEventPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.offset = buf.readBlockPos();
        this.paramA = buf.readInt();
        this.paramB = buf.readInt();
        this.levelEvent = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(this.playerId);
        buffer.writeBlockPos(this.offset);
        buffer.writeInt(this.paramA);
        buffer.writeInt(this.paramB);
        buffer.writeBoolean(this.levelEvent);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void handle(Player nulled) {
        ClientLevel lv = Minecraft.getInstance().level;
        Entity pl = lv.getEntity(this.playerId);
        if (!this.levelEvent) {
            if (pl instanceof PlayerAccessor accessor) {
                UseController ctr = accessor.getUseControllers().get(this.offset);
                if (ctr != null) {
                    UseLevel lv2 = new UseLevel(ctr.getOwner().level(), ctr.getOwner().level().isClientSide, ctr);
                    ctr.getBlockState().triggerEvent(lv2, ctr.getOffset(), this.paramA, this.paramB);
                }
            }
        } else {
            Player player1 = null;
            if (pl instanceof Player player2) player1 = player2;
            if (!this.handleCustomBlockFix(player1) && player1 != null) {
                Minecraft.getInstance().levelRenderer.levelEvent(this.paramA, player1.blockPosition().offset(this.offset), this.paramB);
            }
        }
    }

    private boolean handleCustomBlockFix(Player player) {
        UseController ctr = null;
        if (player instanceof PlayerAccessor accessor) {
            ctr = accessor.getUseControllers().get(this.offset);
        }
        switch (this.paramA) {
            case -1:
                if (ctr != null) {
                    BlockEntity ent = ctr.getBlockEntity();
                    if (ent instanceof SignBlockEntity signBlockEntity) {
                        Minecraft.getInstance().player.openTextEdit(signBlockEntity, this.paramB != 0);
                    }
                }
                return true;
            case -2:
                if (ctr != null) {
                    PlayerJukeboxSoundInstance.play(ctr, this.paramB);
                }
                return true;
            case -3:
                if (ctr != null) {
                    PlayerJukeboxSoundInstance.stop(ctr);
                }
                return true;
            case -4:
                if (player != null)
                    PlayerJukeboxSoundInstance.stopAll(this.playerId);
                return true;
            default:
                return false;
        }
    }
}
