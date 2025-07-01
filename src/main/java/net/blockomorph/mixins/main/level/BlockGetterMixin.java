package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockGetter.class)
public interface BlockGetterMixin {

	@ModifyVariable(method = "boxTraverseBlocks", at = @At(value = "HEAD"), ordinal = 1)
	private static Vec3 normalize(Vec3 orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}

	@ModifyVariable(method = "boxTraverseBlocks", at = @At(value = "HEAD"), ordinal = 0)
	private static Vec3 normalize2(Vec3 orig) {
		return InPlayerBlockPos.checkOnReal(orig);
	}
}