package net.blockomorph.core.levelFlags;

public interface LevelWithFlags {
	LevelWithFlags EMPTY = new LevelWithFlags() {
		private final MorphedLevelFeatureFlags dummy = new MorphedLevelFeatureFlags();
		@Override public MorphedLevelFeatureFlags flags() { return this.dummy; }
	};
	MorphedLevelFeatureFlags flags();
	static LevelWithFlags of(Object lv) {
		if (lv instanceof LevelWithFlags fl) return fl;
		return EMPTY;
	}
}
