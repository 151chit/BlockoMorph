package net.blockomorph.platformUtilsImpl;

import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricEarlyUtils implements EarlyLoadingPlatformUtils {

	@Override
	public boolean isModLoaded(String string) {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader == null) return false;
		return loader.isModLoaded(string);
	}

	@Override
	public Path getGameDir() {
		return FabricLoader.getInstance().getGameDir();
	}
}
