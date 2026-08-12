package net.blockomorph.utils.mixin;

import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FastInjector extends Injector {
	static final Class<? extends Annotation> PRIMITIVE_RETURN = PrimitiveCancelSignal.class;
	static final Class<? extends Annotation> INJECTOR = FastInject.class;
	private static final Type OBJECT = Type.getType(Object.class);
	private final AnnotationNode primitiveReturn;

	public FastInjector(InjectionInfo info, AnnotationNode primitiveReturn) {
		super(info, "@" + INJECTOR.getSimpleName());
		this.primitiveReturn = primitiveReturn;
	}

	@Override
	protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
		super.sanityCheck(target, injectionPoints);
		this.checkTargetModifiers(target, true);
	}

	//VERSION DEPEND PART \/
	@SuppressWarnings("unused")
	protected void addTargetNode(InjectorTarget injectorTarget, List<InjectionNodes.InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
		this.checkConstructors(injectorTarget.getTarget(), myNodes, node, nominators);
	}

	@SuppressWarnings("unused")
	protected void addTargetNode(Target target, List<InjectionNodes.InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
		this.checkConstructors(target, myNodes, node, nominators);
	}
	//-------------------//

	private void checkConstructors(Target target, List<InjectionNodes.InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
		InjectionNodes.InjectionNode injectionNode = target.addInjectionNode(node);

		for (InjectionPoint ip : nominators) {
			try {
				this.checkTargetForNode(target, injectionNode, ip.getTargetRestriction(this.info));
			} catch (InvalidInjectionException ex) {
				throw new InvalidInjectionException(this.info, String.format("%s selector %s", ip, ex.getMessage()));
			}
		}

		myNodes.add(injectionNode);
	}

	private void validateDescriptor(Target target) {
		if (!Arrays.equals(this.methodArgs, target.arguments)) {
			throw new InvalidInjectionException(this.info, "Invalid args, need: " + Arrays.toString(target.arguments) + ", found: " + Arrays.toString(this.methodArgs));
		}
		boolean needPrimitiveReturn = false;
		Type gameMethodReturn = target.returnType;
		if (gameMethodReturn.equals(Type.VOID_TYPE)) {
			this.checkDesc(Type.BOOLEAN_TYPE);
		} else if (gameMethodReturn.equals(Type.BOOLEAN_TYPE)) {
			this.checkDesc(Type.BYTE_TYPE);
		} else if (!ASMUtils.isPrimitive(gameMethodReturn)) {
			this.checkDesc(OBJECT);
		} else {
			this.checkDesc(gameMethodReturn);
			needPrimitiveReturn = true;
		}

		if (needPrimitiveReturn) {
			if (this.primitiveReturn == null || this.getAnnotationPrimitiveValue(target) == null) {
				throw new InvalidInjectionException(this.info, PRIMITIVE_RETURN.getSimpleName() + " is missing or an invalid return type is specified!");
			}
		} else {
			if (this.primitiveReturn != null) {
				throw new InvalidInjectionException(this.info, PRIMITIVE_RETURN.getSimpleName() + " is present on a non-primitive method!");
			}
		}
	}

	private void checkDesc(Type need) {
		if (!this.returnType.equals(need)) {
			throw new InvalidInjectionException(this.info, String.format("Handler method must return %s, this: %s", need, this.returnType));
		}
	}

	private Object getAnnotationPrimitiveValue(Target target) {
		String fieldName = switch (target.returnType.getSort()) {
			case Type.INT -> "intValue";
			case Type.FLOAT -> "floatValue";
			case Type.LONG -> "longValue";
			case Type.DOUBLE -> "doubleValue";
			case Type.SHORT -> "shortValue";
			case Type.BYTE -> "byteValue";
			case Type.CHAR -> "charValue";
			default -> throw new IncompatibleClassChangeError("Unsupported primitive type: " + target.returnType);
		};
		return Annotations.getValue(this.primitiveReturn, fieldName);
	}

	@Override
	protected void inject(Target target, InjectionNodes.InjectionNode node) {
		this.validateDescriptor(target);
		this.patchMethod(target, node);
		this.info.notifyInjected(target);
	}

	private void patchMethod(Target target, InjectionNodes.InjectionNode node) {
		InsnList instructions = new InsnList();

		this.invokeHandlerWithArgs(target.arguments, instructions, target.getArgIndices());
		Type gameMethodReturn = target.returnType;
		if (gameMethodReturn.equals(Type.VOID_TYPE)) {
			this.injectVoid(instructions);
		} else if (gameMethodReturn.equals(Type.BOOLEAN_TYPE)) {
			this.injectBoolean(instructions);
		} else if (!ASMUtils.isPrimitive(gameMethodReturn)) {
			this.injectObject(target.returnType, instructions);
		} else {
			this.injectPrimitive(target, instructions);
		}
		target.insertBefore(node, instructions);
	}

	private void injectObject(Type returnType, InsnList instructions) {
		LabelNode skipReturn = new LabelNode();
		instructions.add(new InsnNode(Opcodes.DUP));
		instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(INJECTOR), FastInjectionInfo.OBJECT_CONTINUE, OBJECT.getDescriptor()));

		instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, skipReturn));
		instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
		instructions.add(new InsnNode(Opcodes.ARETURN));

		instructions.add(skipReturn);
		instructions.add(new InsnNode(Opcodes.POP));
	}

	private void injectVoid(InsnList instructions) {
		LabelNode skipReturn = new LabelNode();
		instructions.add(new JumpInsnNode(Opcodes.IFNE, skipReturn));
		instructions.add(new InsnNode(Opcodes.RETURN));

		instructions.add(skipReturn);
	}

	private void injectBoolean(InsnList instructions) {
		LabelNode skipReturn = new LabelNode();
		instructions.add(new InsnNode(Opcodes.DUP));
		instructions.add(new JumpInsnNode(Opcodes.IFEQ, skipReturn));

		LabelNode isNegative = new LabelNode();
		instructions.add(new JumpInsnNode(Opcodes.IFLT, isNegative));
		instructions.add(new InsnNode(Opcodes.ICONST_1));
		instructions.add(new InsnNode(Opcodes.IRETURN));

		instructions.add(isNegative);
		instructions.add(new InsnNode(Opcodes.ICONST_0));
		instructions.add(new InsnNode(Opcodes.IRETURN));

		instructions.add(skipReturn);
		instructions.add(new InsnNode(Opcodes.POP));
	}

	private void injectPrimitive(Target target, InsnList instructions) {
		Object signal = this.getAnnotationPrimitiveValue(target);
		Type type = target.returnType;
		instructions.add(new InsnNode(type.getSize() == 1 ? Opcodes.DUP : Opcodes.DUP2));

		boolean isNaN = (signal instanceof Float nf && nf.isNaN()) || (signal instanceof Double nd && nd.isNaN());

		LabelNode skip = new LabelNode();
		if (isNaN) {
			boolean isFloat = signal instanceof Float;
			instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
					"java/lang/" + (isFloat ? "Float" : "Double"),
					"isNaN",
					(isFloat ? "(F" : "(D") + ")Z", false));
			instructions.add(new JumpInsnNode(Opcodes.IFNE, skip));
		} else this.injectClassicNumberCheck(type, instructions, signal, skip);

		instructions.add(new InsnNode(type.getOpcode(Opcodes.IRETURN)));

		instructions.add(skip);
		instructions.add(new InsnNode(type.getSize() == 1 ? Opcodes.POP : Opcodes.POP2));
	}

	private void injectClassicNumberCheck(Type returnType, InsnList instructions, Object signal, LabelNode skip) {
		this.pushConstant(instructions, signal);
		if (returnType.getSort() == Type.LONG) {
			instructions.add(new InsnNode(Opcodes.LCMP));
			instructions.add(new JumpInsnNode(Opcodes.IFEQ, skip));
		} else if (returnType.getSort() == Type.FLOAT) {
			instructions.add(new InsnNode(Opcodes.FCMPG));
			instructions.add(new JumpInsnNode(Opcodes.IFEQ, skip));
		} else if (returnType.getSort() == Type.DOUBLE) {
			instructions.add(new InsnNode(Opcodes.DCMPG));
			instructions.add(new JumpInsnNode(Opcodes.IFEQ, skip));
		} else {
			instructions.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, skip));
		}
	}

	private void pushConstant(InsnList insts, Object value) {
		if (value instanceof Integer) insts.add(this.getConstantInsn((int)value));
		else if (value instanceof Long) insts.add(new LdcInsnNode(value));
		else if (value instanceof Float) insts.add(new LdcInsnNode(value));
		else if (value instanceof Double) insts.add(new LdcInsnNode(value));
		else if (value instanceof Byte) insts.add(this.getConstantInsn((byte)value));
		else if (value instanceof Short) insts.add(this.getConstantInsn((short)value));
		else if (value instanceof Character) insts.add(this.getConstantInsn((char)value));
	}

	protected AbstractInsnNode getConstantInsn(int value) {
		if (value > -1 && value <= 5) {
			return new InsnNode(Opcodes.ICONST_0 + value);
		} else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			return new IntInsnNode(Opcodes.BIPUSH, value);
		} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			return new IntInsnNode(Opcodes.SIPUSH, value);
		}
		return new LdcInsnNode(value);
	}
}
