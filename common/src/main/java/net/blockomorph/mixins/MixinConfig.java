package net.blockomorph.mixins;

import net.blockomorph.utils.platform.EarlyLoadingPlatformUtils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MixinConfig implements IMixinConfigPlugin {
	private static final String COMPAT_PACKAGE = "net.blockomorph.mixins.compat.";
	private final Map<String, Boolean> LOADED_MODS = new HashMap<>();
	private static final Map<String, String> PACKAGE_TO_MODID = Map.of(
			"flywheel", "flywheel",
			"create", "create",
			"vs2", "valkyrienskies",
			"lithium", "lithium",
			"sodium", "sodium",
			"ebe", "enhancedblockentities",
			"betterF3", "betterf3"
	);

	@Override
	public void onLoad(String s) {
		LOADED_MODS.clear();
		PACKAGE_TO_MODID.forEach((packageName, modId) -> {
			LOADED_MODS.put(modId, EarlyLoadingPlatformUtils.INSTANCE.isModLoaded(modId));
		});
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String className, String mixinClassName) {
		if (!mixinClassName.startsWith(COMPAT_PACKAGE)) {
			return true;
		}
		try {
			String relativePath = mixinClassName.substring(COMPAT_PACKAGE.length());
			String subPackage = relativePath.split("\\.")[0];

			String targetModId = PACKAGE_TO_MODID.get(subPackage);
			if (targetModId != null) {
				return LOADED_MODS.getOrDefault(targetModId, false);
			}
		} catch (Throwable e) {
			throw new IllegalArgumentException(e);
		}

		return false;
	}

	@Override
	public void acceptTargets(Set<String> set, Set<String> set1) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
	}

	@Override
	public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
	}
}
