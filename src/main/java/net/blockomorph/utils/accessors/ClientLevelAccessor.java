package net.blockomorph.utils.accessors;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

public interface ClientLevelAccessor {
	boolean blockEntityRendering();
	void setBlockEntityRenderingMode(boolean yes);

	static ClientLevelAccessor of(Level lv) {
		if (lv instanceof ClientLevelAccessor acc)
			return acc;
		return NULL;
	}

	ClientLevelAccessor NULL = new ClientLevelAccessor() {
		@Override
		public boolean blockEntityRendering() { return false; }
		@Override
		public void setBlockEntityRenderingMode(boolean yes) {}
	};
}
