package net.blockomorph.utils.config.enums;

public class ConfigEnums {
	private ConfigEnums() {
		throw new UnsupportedOperationException();
	}

	public enum Mode {
		NONE,
		BLACKLIST,
		WHITELIST
	}

	public enum ScreenAccess {
		NONE(false, false),
		MORPH_SCREEN(true, false),
		CONFIG_MORPH_SCREEN(false, true),
		ALL(true, true);

		public final boolean morph;
		public final boolean config;

		ScreenAccess(boolean morph, boolean config) {
			this.morph = morph;
			this.config = config;
		}
	}

	public enum HitReaction {
		DISABLED(false, false),
		BRAKING(false, false),
		MELEE(true, false),
		PROJECTILES(false, true),
		FULL_PVP(true, true);

		public final boolean hand;
		public final boolean projectile;

		HitReaction(boolean hand, boolean projectile) {
			this.hand = hand;
			this.projectile = projectile;
		}
	}
}
