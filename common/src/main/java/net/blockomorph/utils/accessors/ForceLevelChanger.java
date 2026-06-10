package net.blockomorph.utils.accessors;

import net.minecraft.world.level.Level;

public interface ForceLevelChanger {
	void forceLevelChange(Level lv);

	static ForceLevelChanger of(Object object) {
		return (ForceLevelChanger) object;
	}
}