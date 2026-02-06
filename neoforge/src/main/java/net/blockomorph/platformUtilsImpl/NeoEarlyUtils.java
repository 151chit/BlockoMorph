package net.blockomorph.platformUtilsImpl;

import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Path;

public class NeoEarlyUtils implements EarlyLoadingPlatformUtils {

	@Override
	public boolean isModLoaded(String modId) {
		LoadingModList modList = FMLLoader.getCurrent().getLoadingModList();
		if (modList == null) return false;
		return modList.getModFileById(modId) != null;
	}

	@Override
	public Path getGameDir() {
		return FMLPaths.GAMEDIR.get();
	}
}
