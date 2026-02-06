package net.blockomorph.utils.accessors;

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

public interface ClientLevelAccessor {
	boolean specialRenderingMode();

	void setSpecialRenderingMode(boolean yes);

	void lockExternalMorphedBlockGetter(boolean yes);

	boolean isExternalMorphedBlockGetterLocked();

	static ClientLevelAccessor of(BlockAndTintGetter lv) {
		if (lv instanceof ClientLevelAccessor acc)
			return acc;
		return NULL;
	}

	ClientLevelAccessor NULL = new ClientLevelAccessor() {
		@Override
		public boolean specialRenderingMode() {
			return false;
		}

		@Override
		public void setSpecialRenderingMode(boolean yes) {
		}

		@Override
		public void lockExternalMorphedBlockGetter(boolean yes) {
		}

		@Override
		public boolean isExternalMorphedBlockGetterLocked() {
			return false;
		}
	};
}
