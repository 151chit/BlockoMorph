package net.blockomorph.platformImpl;

import net.blockomorph.utils.EarlyLoadingPlatformService;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricEarlyService implements EarlyLoadingPlatformService {

	@Override
	public boolean isModLoaded(String string) {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader == null) return false;
		return loader.isModLoaded(string);
	}

	@Override
	public boolean isRunningInIde() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public Path getGameDir() {
		return FabricLoader.getInstance().getGameDir();
	}
}
