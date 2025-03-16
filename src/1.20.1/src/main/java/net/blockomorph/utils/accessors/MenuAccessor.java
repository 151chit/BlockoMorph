package net.blockomorph.utils.accessors;

import net.minecraft.world.entity.player.Player;

public interface MenuAccessor {
    void boundToPlayer(Player pl);
    Player getPlayer();
}
