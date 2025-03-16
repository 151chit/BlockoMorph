package net.blockomorph;

import net.blockomorph.core.MainBus;
import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockomorphServer implements DedicatedServerModInitializer {
	public static final String MOD_ID = "blockomorph";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeServer() {
		MainBus.registerServer();
	}
}
