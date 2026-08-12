package net.blockomorph.mixins.main.system.inPlayerManager.sectionStorage;

import net.blockomorph.core.storage.playerSection.PlayersMultiSectionStorage;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Level.class)
public abstract class LevelMixin implements PlayersStorage.Provider {
	@Unique private final PlayersMultiSectionStorage playersMultiSectionStorage = new PlayersMultiSectionStorage();

	@Override
	public PlayersStorage getStorage() {
		return this.playersMultiSectionStorage;
	}
}