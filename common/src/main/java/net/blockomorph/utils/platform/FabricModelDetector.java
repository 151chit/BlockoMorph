package net.blockomorph.utils.platform;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.client.renderer.block.dispatch.multipart.MultiPartModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Predicate;

public class FabricModelDetector {
	private static final Set<Class<?>> VANILLA_MODELS =
			Set.of(SingleVariant.class, MultiPartModel.class, WeightedVariants.class);//CHECK AFTER MINECRAFT UPDATE!!!
	private static final ClassValue<Boolean> IS_FABRIC_MODEL = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			if (type == null || type == Object.class) return false;
			if (VANILLA_MODELS.contains(type)) return false;
			String className = type.getName();
			if (className.startsWith("net.neoforged.neoforge") || className.startsWith("net.fabricmc.fabric")) return false; //check on update

			for (var method : type.getDeclaredMethods()) {
				if (isEmitQuadsMethod(method)) {
					return true;
				}
			}

			for (Class<?> iFaces : type.getInterfaces()) {
				if (this.get(iFaces)) return true;
			}
			Class<?> superType = type.getSuperclass();
			if (superType == null) return false;
			return this.get(superType);
		}
	};

	private static boolean isEmitQuadsMethod(Method method) { //check on update
		if (method.getReturnType() != void.class) return false;
		if (Modifier.isStatic(method.getModifiers())) return false;
		Class<?>[] params = method.getParameterTypes();
		if (params.length != 6) return false;
		if (!method.getName().equals("emitQuads")) return false;
		return params[0].getName().contains("QuadEmitter") &&
				params[1].equals(BlockAndTintGetter.class) &&
				params[2].equals(BlockPos.class) &&
				params[3].equals(BlockState.class) &&
				params[4].equals(RandomSource.class) &&
				params[5].equals(Predicate.class);
	}

	public static boolean isFabricModel(Class<?> blockModel) {
		return IS_FABRIC_MODEL.get(blockModel);
	}
}
