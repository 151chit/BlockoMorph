package net.blockomorph;

import net.blockomorph.registryLegacy.ClientRegister;
import net.fabricmc.api.ClientModInitializer;

public class BlockomorphClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientRegister.register();
	}
}
