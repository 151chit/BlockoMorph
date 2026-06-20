package net.blockomorph.interfaceInjectionSupport;

import com.llamalad7.mixinextras.utils.MixinInternals;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class InterfaceInjectorsApplicatorExtension implements IExtension {
	private static final Logger LOGGER = LoggerFactory.getLogger(InterfaceInjectorsApplicatorExtension.class.getName() + "_blockomorph");
	public static final InterfaceInjectorsApplicatorExtension INSTANCE = new InterfaceInjectorsApplicatorExtension();
	private InterfaceInjectorsApplicatorExtension() {
		LOGGER.info("Try run interface injections on mixin version: " + MixinBootstrap.VERSION);
	}
	private static final MethodHandle MIXIN_TARGET_CONTEXT_CTR;
	private static final MethodHandle GET_SESSION_ID;
	private static final MethodHandle GET_CLASS_UID;
	private static final MethodHandle GET_METHOD_UID;
	private static final String WRAPPER_DESC = Type.getDescriptor(InjectorWrapper.class);
	private final Map<ITargetClassContext, Collection<InjectionInfo>> INJECTOR_QUEUE = new HashMap<>();

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}

	@Override
	public void preApply(ITargetClassContext context) {
		for (var pair : MixinInternals.getMixinsFor(context)) {
			IMixinInfo mixinInfo = pair.getKey();
			ClassNode mixinClass = pair.getRight();
			if (!Modifier.isInterface(mixinClass.access)) continue;
			MixinTargetContext ctx = null;
			Set<MethodNode> handlers = new HashSet<>();
			for (MethodNode mixinMethod : mixinClass.methods) {
				if (mixinMethod.visibleAnnotations == null) continue;
				for (AnnotationNode wrapper : mixinMethod.visibleAnnotations) {
					if (wrapper.desc.equals(WRAPPER_DESC)) {
						AnnotationNode injector = Annotations.getValue(wrapper, "value");
						if (injector == null)
							throw new IllegalArgumentException("Empty annotation wrapper on method: " + mixinMethod.name + mixinMethod.desc + " in class: " + context.getClassNode().name);
						if (ctx == null) {
							ctx = call(MIXIN_TARGET_CONTEXT_CTR, mixinInfo, mixinClass, context);
						}
						this.handlePreAnnotation(context, handlers, mixinMethod, ctx, injector);
						break;
					}
				}
			}
			handlers.forEach(m -> context.getClassNode().methods.add(m));
		}
	}

	private void handlePreAnnotation(ITargetClassContext context, Set<MethodNode> handlers, MethodNode mixinMethod, MixinTargetContext ctx, AnnotationNode injector) {
		mixinMethod.visibleAnnotations = new ArrayList<>(mixinMethod.visibleAnnotations);
		mixinMethod.visibleAnnotations.add(injector);
		this.repairAccessModifier(mixinMethod, context, false);
		InjectionInfo info = InjectionInfo.parse(ctx, mixinMethod);
		mixinMethod.name = this.getHandlerName(mixinMethod, info);
		handlers.add(mixinMethod);
		this.swapAnnotation(mixinMethod, ctx, context, injector);
		info.prepare();
		info.preInject();
		mixinMethod.access = ACC_PUBLIC;
		INJECTOR_QUEUE.computeIfAbsent(context, l -> new ArrayList<>()).add(info);
	}

	private void swapAnnotation(MethodNode methodNode, MixinTargetContext ctx, ITargetClassContext clTr, AnnotationNode injector) {
		methodNode.visibleAnnotations.remove(injector);
		Annotations.setVisible(methodNode, MixinMerged.class,
				"mixin", ctx.getClassName(),
				"priority", ctx.getPriority(),
				"sessionId", call(GET_SESSION_ID, clTr));
	}

	private void repairAccessModifier(MethodNode mixinMethod, ITargetClassContext context, boolean removeWrapper) {
		for (AnnotationNode wrapper : mixinMethod.visibleAnnotations) {
			if (wrapper.desc.equals(WRAPPER_DESC)) {
				Integer access = Annotations.getValue(wrapper, "accessModifier");
				if (access == null) {
					throw new IllegalArgumentException("Unknown access modifier on method: " + mixinMethod.name + mixinMethod.desc + " in class: " + context.getClassNode().name);
				}
				mixinMethod.access = access;
				if (removeWrapper) {
					mixinMethod.visibleAnnotations = new ArrayList<>(mixinMethod.visibleAnnotations);
					mixinMethod.visibleAnnotations.remove(wrapper);
				}
				return;
			}
		}
	}

	private String getHandlerName(MethodNode mixinMethod, InjectionInfo info) {
		String prefix = InjectionInfo.getInjectorPrefix(info.getAnnotationNode());
		String classUID = call(GET_CLASS_UID, info.getMixin().getClassRef());
		String methodUID = call(GET_METHOD_UID, mixinMethod.name, mixinMethod.desc, false);
		return String.format("%s$%s%s$%s", prefix, classUID, methodUID, mixinMethod.name);
	}

	@SuppressWarnings("unchecked")
	private static <T> T call(MethodHandle handle, Object... args) {
		try {
			return (T) handle.invokeWithArguments(args);
		} catch (Throwable e) {
			throw new RuntimeException("An error occurred while apply interface injection", e);
		}
	}

	@Override
	public void postApply(ITargetClassContext context) {
		Collection<InjectionInfo> injectionInfos = INJECTOR_QUEUE.remove(context);
		if (injectionInfos != null) for (InjectionInfo info : injectionInfos) {
			var targetsMap = MixinInternals.getTargets(info);
			info.inject();
			info.postInject();
			String nameDesc = info.getMethod().name + info.getMethod().desc;
			for (MethodNode methodNode : context.getClassNode().methods) {
				String realNameDesc = methodNode.name + methodNode.desc;
				if (realNameDesc.equals(nameDesc)) {
					this.repairAccessModifier(methodNode, context, true);
					break;
				}
			}
			this.repairCallers(targetsMap, context.getClassNode(), info.getMethod());
			LOGGER.info("Interface injector applied to class: " +
					"{} From mixin: {} For method: {}", context.getClassNode().name, info.getMixin().getClassName(), info.getMethod().name);
		}
	}

	private void repairCallers(Map<Target, List<InjectionNodes.InjectionNode>> targets, ClassNode targetClass, MethodNode handlerMethod) {
		for (Target target : targets.keySet()) {
			if (target.classNode.name.equals(targetClass.name)) {
				AbstractInsnNode insn = target.insns.getFirst();
				while (insn != null) {
					if (insn instanceof MethodInsnNode caller) {
						if (caller.name.equals(handlerMethod.name) && caller.desc.equals(handlerMethod.desc)) {
							caller.itf = true;
							if (caller.getOpcode() != Opcodes.INVOKESTATIC) caller.setOpcode(Opcodes.INVOKEINTERFACE);
						}
					}
					insn = insn.getNext();
				}
			}
		}
	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {}

	static {
		try {
			MethodHandles.Lookup baseLookup = MethodHandles.lookup();
			Class<?> mixinInfoCls = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo");
			Class<?> targetClCtxCls = Class.forName("org.spongepowered.asm.mixin.transformer.TargetClassContext");

			var l1 = MethodHandles.privateLookupIn(MixinTargetContext.class, baseLookup);
			MIXIN_TARGET_CONTEXT_CTR = l1.findConstructor(MixinTargetContext.class,
					MethodType.methodType(void.class, mixinInfoCls, ClassNode.class, targetClCtxCls));

			var l2 = MethodHandles.privateLookupIn(targetClCtxCls, baseLookup);
			GET_SESSION_ID = l2.findVirtual(targetClCtxCls, "getSessionId",
					MethodType.methodType(String.class));

			Class<?> mapperCls = Class.forName("org.spongepowered.asm.mixin.transformer.MethodMapper");
			var l3 = MethodHandles.privateLookupIn(mapperCls, baseLookup);

			GET_CLASS_UID = l3.findStatic(mapperCls, "getClassUID",
					MethodType.methodType(String.class, String.class));

			GET_METHOD_UID = l3.findStatic(mapperCls, "getMethodUID",
					MethodType.methodType(String.class, String.class, String.class, boolean.class));
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException("Mod was broken, please report to https://github.com/151chit/BlockoMorph/issues, mixin version: " +
					MixinBootstrap.VERSION, e);
		}
	}
}