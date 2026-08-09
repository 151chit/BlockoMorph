package net.blockomorph.utils.mixin;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

@InjectionInfo.AnnotationType(FastInject.class)
@InjectionInfo.HandlerPrefix("fastInject")
public class FastInjectionInfo extends InjectionInfo {
	static final String OBJECT_CONTINUE = "CONTINUE_EXECUTION";
	public FastInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
		super(mixin, method, annotation);
	}

	@Override
	protected Injector parseInjector(AnnotationNode injectAnnotation) {
		AnnotationNode primitiveReturn = Annotations.getValue(injectAnnotation, "continueIf", FastInjector.PRIMITIVE_RETURN);
		return new FastInjector(this, primitiveReturn);
	}

	@Override
	protected String getDescription() {
		return "Method without a callback object";
	}
}