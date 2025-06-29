package net.blockomorph.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;
import java.util.function.Function;

@Debug(export = true)
@Mixin(BlockGetter.class)
public interface TestMixin {

	@Inject(method = "clip", at = @At("HEAD"))
	default void run(ClipContext p_45548_, CallbackInfoReturnable<BlockHitResult> cir) {

	}

	@Inject(method = "traverseBlocks", at = @At("HEAD"))
	private static <T, C> void run2(Vec3 p_151362_, Vec3 p_151363_, C p_151364_, BiFunction<C, BlockPos, T> p_151365_, Function<C, T> p_151366_, CallbackInfoReturnable<T> cir) {

	}
}
