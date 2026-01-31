package net.blockomorph;

import net.blockomorph.core.CommonRegister;
import net.fabricmc.api.ModInitializer;

public class BlockomorphCommon implements ModInitializer {

	@Override
	public void onInitialize() {
		CommonRegister.register();
	}
}
