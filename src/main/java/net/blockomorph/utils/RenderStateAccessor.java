package net.blockomorph.utils;

import net.minecraft.client.player.AbstractClientPlayer;

public interface RenderStateAccessor {
   void loadPlayer(AbstractClientPlayer pl);
   AbstractClientPlayer getPlayer();
}
