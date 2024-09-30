package net.blockomorph;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.fabricmc.api.ModInitializer;

import net.blockomorph.core.MainBus;

public class BlockomorphMod implements ModInitializer {
	public static final String MOD_ID = "blockomorph";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		MainBus.registerServer();
	}
}
