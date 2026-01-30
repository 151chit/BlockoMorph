package net.blockomorph.core;

import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.platform.RegisterPlatformUtils;

public class ClientRegister {

	public static void register() {
		Config.dummyInit();
		KeyMappings.registerKeyMappings(RegisterPlatformUtils.INSTANCE::registerKeyMappings);
	}
}
