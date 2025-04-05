package net.blockomorph.network;

import net.blockomorph.utils.config.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ServerBoundConfigUpdatePacket implements BlockMorphPacket {
    public static final String ID = "server_bound_config_update_packet";
    String option;
    String value;
    public ServerBoundConfigUpdatePacket(String option, String val) {
        this.option = option;
        this.value = val;
    }

    public ServerBoundConfigUpdatePacket(FriendlyByteBuf buf) {
        this.option = buf.readUtf();
        this.value = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.option);
        buffer.writeUtf(this.value);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void handle(Player player) {
        if (player.hasPermissions(2) && (boolean) Config.getInstance().getValue("canOperatorModifyConfig")) {
            Config.getInstance().parse(this.option, this.value, true);
        }
    }
}
