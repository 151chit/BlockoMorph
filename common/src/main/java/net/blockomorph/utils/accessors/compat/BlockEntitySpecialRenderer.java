package net.blockomorph.utils.accessors.compat;

import net.blockomorph.mixins.compat.bbe.BBEconfigAccessor;
import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import oshi.annotation.concurrent.NotThreadSafe;

public interface BlockEntitySpecialRenderer {
	boolean BBE_LOADED = EarlyLoadingPlatformUtils.INSTANCE.isModLoaded("betterblockentities");

	@NotThreadSafe
	static void tryRenderWithoutOptimizations(Runnable action) {
		if (!BBE_LOADED) {
			action.run();
			return;
		}
		boolean isOptimized = BBEconfigAccessor.isOptimized();
		BBEconfigAccessor.setMasterOptimize(false);
		action.run();
		BBEconfigAccessor.setMasterOptimize(isOptimized);
	}
}