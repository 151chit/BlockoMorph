package net.blockomorph.utils.blockUpdate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;


public enum BlockMethodDetector {
	NEIGHBOUR_UPDATE(() -> {
		class RedstoneUpdate extends DummyBlock {
			@Target public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {}
		}
		return RedstoneUpdate.class;
	});
	//shape not implemented

	@Retention(RetentionPolicy.RUNTIME) public @interface Target {}
	static class DummyBlock extends Block { private DummyBlock() { super(Properties.of()); }}
	private final ClassValue<Boolean> detector;
	BlockMethodDetector(Supplier<Class<? extends DummyBlock>> dummy) {
		String id = methodId(Arrays.stream(dummy.get().getDeclaredMethods())
				.filter(m -> m.isAnnotationPresent(Target.class)).findFirst().orElseThrow());
		this.detector = new ClassValue<>() {
			@Override
			protected Boolean computeValue(@NotNull Class<?> type) {
				if (type == BlockBehaviour.class) return false;
				if (type.isPrimitive()) return false;
				if (!BlockBehaviour.class.isAssignableFrom(type)) return false;
				for (Method method : type.getDeclaredMethods()) {
					if (id.equals(methodId(method))) {
						return true;
					}
				}
				return this.get(type.getSuperclass());
			}
		};
	}

	private static String methodId(Method method) {
		return method.getName() + Type.getMethodDescriptor(method);
	}

	public boolean hasMethodInClass(Block block) {
		return this.detector.get(block.getClass());
	}
}
