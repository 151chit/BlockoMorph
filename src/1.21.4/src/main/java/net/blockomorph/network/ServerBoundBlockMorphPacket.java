package net.blockomorph.network;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class ServerBoundBlockMorphPacket implements BlockMorphPacket {
    public static final String ID = "server_bound_block_morph_packet";
    CompoundTag tag;
    private ServerBoundBlockMorphPacket(CompoundTag nbt) {
        this.tag = nbt;
    }

    public ServerBoundBlockMorphPacket(FriendlyByteBuf buf) {
        this.tag = buf.readNbt();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(this.tag);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void handle(Player player) {
        try {
            if (player instanceof PlayerAccessor mob) {
                if (tag == null) throw new IllegalArgumentException("Nbt is null!");
                if (tag.contains("fuse", 1)) {
                    mob.setTnt();
                    return;
                }
                BlockState blockstate = NbtUtils.readBlockState(player.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
                MorphUtils.BannedBlock reason = MorphUtils.isBannedBlock(blockstate, player);
                if (reason == null) {
                    CompoundTag nbt = tag.getCompound("Tags");
                    if (tag.contains("MultiBlock", 1) && (boolean) Config.getInstance().getValue("advancedMode")) {
                        mob.applyBlockMorph(blockstate, nbt, tag.getBoolean("MultiBlock"));
                    } else {
                        mob.applyBlockMorph(blockstate, nbt);
                    }
                } else throw new IllegalArgumentException(reason.reason());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid block morph nbt from player " + player + ": " + e.getMessage());
        }
    }

    public static ServerBoundBlockMorphPacket create(BlockState state, CompoundTag tagMorph) {
        CompoundTag tag = new CompoundTag();
        tag.put("BlockState", NbtUtils.writeBlockState(state));
        tag.put("Tags", tagMorph);
        return new ServerBoundBlockMorphPacket(tag);
    }

    public static ServerBoundBlockMorphPacket create(BlockState state, CompoundTag tagMorph, boolean mb) {
        CompoundTag tag = new CompoundTag();
        tag.put("BlockState", NbtUtils.writeBlockState(state));
        tag.put("Tags", tagMorph);
        tag.putBoolean("MultiBlock", mb);
        return new ServerBoundBlockMorphPacket(tag);
    }

    public static ServerBoundBlockMorphPacket fuse() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("fuse", true);
        return new ServerBoundBlockMorphPacket(tag);
    }
}
