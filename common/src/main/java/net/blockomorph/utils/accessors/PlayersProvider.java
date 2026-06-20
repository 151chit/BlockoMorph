package net.blockomorph.utils.accessors;

import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.world.level.Level;

public interface PlayersProvider {
	PlayersMultiSectionStorage getStorage$blockomorph();

	static PlayersProvider of(Level lv) {
		return (PlayersProvider) lv;
	}
}
