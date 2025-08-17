package net.blockomorph;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;

import net.blockomorph.core.MainBus;

@Deprecated(since = "4.0.4")
@Environment(EnvType.CLIENT)
public class BlockomorphClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MainBus.registerClient();
	}
}
