package net.blockomorph.fabric;

import net.fabricmc.loom.api.remapping.RemapperContext;
import net.fabricmc.loom.api.remapping.RemapperExtension;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import net.fabricmc.loom.api.remapping.TinyRemapperExtension;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.gradle.api.logging.Logging;
import org.objectweb.asm.ClassVisitor;
import org.slf4j.Logger;

@SuppressWarnings("UnstableApiUsage")
public class FastInjectRemapperHolder implements RemapperExtension<RemapperParameters.None>, TinyRemapperExtension {
	private static final Logger LOGGER = Logging.getLogger(FastInjectRemapperHolder.class);

	@Override
	public TinyRemapper.ApplyVisitorProvider getPreApplyVisitor(Context context) {
		LOGGER.error("Successfully registered fabric remapper for @FastInject: {}", context);// 'error' for red highlight
		return (ctrClass, visitor) -> new FastInjectDataRemapper(ctrClass.getEnvironment(), visitor);
	}

	@Override
	public ClassVisitor insertVisitor(String s, RemapperContext remapperContext, ClassVisitor classVisitor) {
		return classVisitor;
	}
}
