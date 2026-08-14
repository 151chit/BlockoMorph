package net.blockomorph.platformImpl;

import net.blockomorph.utils.EarlyLoadingPlatformService;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Path;

public class NeoEarlyService implements EarlyLoadingPlatformService {

	@Override
	public boolean isModLoaded(String modId) {
		LoadingModList modList = LoadingModList.get();
		if (modList == null) return false;
		return modList.getModFileById(modId) != null;
	}

	@Override
	public boolean isRunningInIde() {
		return !FMLEnvironment.production;
	}

	@Override
	public Path getGameDir() {
		return FMLPaths.GAMEDIR.get();
	}
}
