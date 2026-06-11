package net.blockomorph;

import net.blockomorph.core.CommonRegister;
import net.fabricmc.api.ModInitializer;

@Deprecated(since = "7.0.4")
public class BlockomorphCommon implements ModInitializer {

	@Override
	public void onInitialize() {
		CommonRegister.register();
	}
}
