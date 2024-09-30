package net.blockomorph;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;

import net.blockomorph.core.MainBus;

@Environment(EnvType.CLIENT)
public class BlockomorphModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MainBus.registerClient();
	}
}
