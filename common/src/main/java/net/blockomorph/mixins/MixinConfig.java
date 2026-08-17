package net.blockomorph.mixins;

import net.blockomorph.utils.mixin.FastInjectionInfo;
import net.blockomorph.utils.EarlyLoadingPlatformService;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

import java.util.List;
import java.util.Set;

public class MixinConfig implements IMixinConfigPlugin {
	private String compatPackage;

	@Override
	public void onLoad(String mixinPackage) {
		this.compatPackage = mixinPackage + ".compat.";
		InjectionInfo.register(FastInjectionInfo.class);
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!mixinClassName.startsWith(this.compatPackage)) return true;
		String remaining = mixinClassName.substring(this.compatPackage.length());

		int firstLayer = remaining.indexOf('.');
		if (firstLayer != -1) {
			String modId = remaining.substring(0, firstLayer);
			return EarlyLoadingPlatformService.INSTANCE.isModLoaded(modId);
		}
		throw new IllegalStateException("Compat mixin in root package: " + mixinClassName);
	}

	@Override public String getRefMapperConfig() { return null; }
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
	@Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
