package net.blockomorph.fabric;

import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Annotation;
import net.fabricmc.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.tinyremapper.extension.mixin.soft.annotation.MixinAnnotationVisitor;
import net.fabricmc.tinyremapper.extension.mixin.soft.annotation.injection.InjectAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

class FastInjectDataRemapper extends ClassVisitor {
	private static final String PACKAGE = FastInjectDataRemapper.class.getPackageName() + ".utils.mixin";
	private static final String FAST_INJECT = PACKAGE + ".FastInject";
	private static final String DESC = "L" + FAST_INJECT.replace(".", "/") + ";";
	private final CommonData data;
	private final List<String> targets = new ArrayList<>();

	FastInjectDataRemapper(TrEnvironment env, ClassVisitor next) {
		super(Opcodes.ASM9, next);
		this.data = new CommonData(env);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		AnnotationVisitor previous = super.visitAnnotation(descriptor, visible);
		if (Annotation.MIXIN.equals(descriptor)) {
			previous = new MixinAnnotationVisitor(this.data, previous, this.targets);
		}
		return previous;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor previous = super.visitMethod(access, name, descriptor, signature, exceptions);
		return this.targets.isEmpty() ? previous : new MethodVisitor(Opcodes.ASM9, previous) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationVisitor delegate = super.visitAnnotation(descriptor, visible);
				if (DESC.contains(descriptor)) {
					return new InjectAnnotationVisitor(data, delegate, targets);
				}
				return delegate;
			}
		};
	}
}