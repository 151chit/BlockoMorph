package net.blockomorph;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.blockomorph.core.MainBus;

@Deprecated(since = "4.0.4")
public class BlockomorphServer implements DedicatedServerModInitializer {
	public static final String MOD_ID = "blockomorph";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeServer() {

		MainBus.registerServer();
	}
}
