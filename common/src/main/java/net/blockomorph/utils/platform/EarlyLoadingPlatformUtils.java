package net.blockomorph.utils.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface EarlyLoadingPlatformUtils {
	EarlyLoadingPlatformUtils INSTANCE = ServiceLoader.load(EarlyLoadingPlatformUtils.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load early platform-depended utils, mod cannot run!"));

	boolean isModLoaded(String modId);
	Path getGameDir();
}
