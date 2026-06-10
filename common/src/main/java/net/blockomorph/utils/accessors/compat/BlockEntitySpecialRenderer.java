package net.blockomorph.utils.accessors.compat;

import jdk.jfr.Description;
import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import oshi.annotation.concurrent.NotThreadSafe;

public interface BlockEntitySpecialRenderer {
	boolean BBE_LOADED = EarlyLoadingPlatformUtils.INSTANCE.isModLoaded("betterblockentities");

	@NotThreadSafe @Description("TODO: Implement this function")
	static void tryRenderWithoutOptimizations(Runnable action) {
		action.run();
	}
}
