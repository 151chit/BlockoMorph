package net.blockomorph.network.blockFix;

import net.blockomorph.network.BlockMorphPacket;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.use.UseController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.TextFilter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerBoundSignUpdatePacket implements BlockMorphPacket {
    public static final String ID = "server_bound_sign_update_packet";
    private static final int MAX_STRING_LENGTH = 384;
    BlockPos pos;
    String[] lines;
    boolean isFrontText;
    int playerId;

    public ServerBoundSignUpdatePacket(int playerId, BlockPos pos, boolean isFrontText, String s1, String s2, String s3, String s4) {
        this.pos = pos;
        this.lines = new String[]{s1, s2, s3, s4};
        this.isFrontText = isFrontText;
        this.playerId = playerId;
    }

    public ServerBoundSignUpdatePacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.isFrontText = buffer.readBoolean();
        this.lines = new String[4];

        for(int i = 0; i < 4; ++i) {
            this.lines[i] = buffer.readUtf(MAX_STRING_LENGTH);
        }
        this.playerId = buffer.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeBoolean(this.isFrontText);

        for(int i = 0; i < 4; ++i) {
            buffer.writeUtf(this.lines[i]);
        }
        buffer.writeInt(this.playerId);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void handle(Player player) {
        if (player.level() instanceof ServerLevel lv) {
            Entity player2 = lv.getEntity(this.playerId);
            if (player2 instanceof PlayerAccessor pl && player2 instanceof ServerPlayer sp) {
                UseController ctr = pl.getUseControllers().get(this.pos);
                if (ctr != null && player.distanceToSqr(sp) < 64) {
                    BlockEntity blockEntity = ctr.getBlockEntity();
                    if (blockEntity instanceof SignBlockEntity sb) {
                        List<String> list = Arrays.asList(this.lines);

                        TextFilter textFilter = sp.getTextFilter();
                        CompletableFuture<List<FilteredText>> future = textFilter.processMessageBundle(list);

                        future.thenAcceptAsync((filteredTexts) -> {
                            sb.updateSignText(player, this.isFrontText, filteredTexts);
                        }, sp.server);
                    }
                }
            }
        }
    }
}
