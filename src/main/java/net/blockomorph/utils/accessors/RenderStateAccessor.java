package net.blockomorph.utils.accessors;

import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.client.player.AbstractClientPlayer;
import org.apache.commons.lang3.mutable.MutableFloat;

public interface RenderStateAccessor {
	void loadPlayer(AbstractClientPlayer pl);

	MutableFloat tick();
	AbstractClientPlayer getPlayer();

	default PlayerAccessor getPl() {
		return (PlayerAccessor) this.getPlayer();
	}
}