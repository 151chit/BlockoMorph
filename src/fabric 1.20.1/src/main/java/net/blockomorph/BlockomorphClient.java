package net.blockomorph;

import net.blockomorph.core.MainBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BlockomorphClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MainBus.registerClient();
	}
}
