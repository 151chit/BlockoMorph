package net.blockomorph.mixins.main.system.inPlayerManager;

import net.blockomorph.core.levelFlags.MorphedLevelFeatureFlags;
import net.blockomorph.core.levelFlags.LevelWithFlags;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public class LevelFlagsMixin implements LevelWithFlags {
	@Unique private final MorphedLevelFeatureFlags flags = new MorphedLevelFeatureFlags();

	@Override
	public MorphedLevelFeatureFlags flags() {
		return this.flags;
	}
}
