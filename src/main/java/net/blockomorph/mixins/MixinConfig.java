package net.blockomorph.mixins;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfig implements IMixinConfigPlugin {
	public static final boolean useFlywheelCompat;
	public static final boolean useVS2compat;
	@Override
	public void onLoad(String s) {}
	@Override
	public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String s, String s1) {
		if (s1.equals("net.blockomorph.mixins.compat.create.Flywheel_BM_BackendMixin")) {
			return useFlywheelCompat;
		} else if (s1.equals("net.blockomorph.mixins.compat.vs2.VS2_BM_VsUtilsMixin") || s1.equals("net.blockomorph.mixins.compat.vs2.VS2_BM_RaycastUtil")) {
			return useVS2compat;
		}
		return true;
	}

	static {
		FabricLoader list = FabricLoader.getInstance();
		if (list != null) {
			useFlywheelCompat = list.isModLoaded("flywheel");
			useVS2compat = list.isModLoaded("valkyrienskies");
		} else {
			useFlywheelCompat = false;
			useVS2compat = false;
		}
	}

	@Override
	public void acceptTargets(Set<String> set, Set<String> set1) {}
	@Override
	public List<String> getMixins() { return null; }
	@Override
	public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
	@Override
	public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
}
