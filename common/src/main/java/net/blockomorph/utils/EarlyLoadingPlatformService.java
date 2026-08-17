package net.blockomorph.utils;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface EarlyLoadingPlatformService {
	EarlyLoadingPlatformService INSTANCE = ServiceLoader.load(EarlyLoadingPlatformService.class).findFirst().orElseThrow(() ->
			new IllegalArgumentException("Failed to load ERALY platform-depended utils, mod cannot run!"));

	boolean isModLoaded(String modId);
	boolean isRunningInIde();
	Path getGameDir();
}
