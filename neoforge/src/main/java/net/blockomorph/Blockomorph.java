package net.blockomorph;

import net.blockomorph.registryLegacy.ClientRegister;
import net.blockomorph.utils.MorphUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MorphUtils.MODID)
public class Blockomorph {
	public Blockomorph() {
		if (FMLEnvironment.getDist() == Dist.CLIENT)
			ClientRegister.register();
	}
}
