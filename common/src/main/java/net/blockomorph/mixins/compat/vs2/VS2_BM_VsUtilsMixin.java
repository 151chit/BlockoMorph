package net.blockomorph.mixins.compat.vs2;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.MorphMath;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt", remap = false) @Debug(export = true)
public abstract class VS2_BM_VsUtilsMixin {

	@ModifyReturnValue(method = "squaredDistanceBetweenInclShips(Lnet/minecraft/world/level/Level;DDDDDD)D", at = @At("RETURN"))
	private static double normalizeCoords(double original,
										  @Local(ordinal = 9) double inWorldX1,
										  @Local(ordinal = 10) double inWorldY1,
										  @Local(ordinal = 11) double inWorldZ1,
										  @Local(ordinal = 12) double inWorldX2,
										  @Local(ordinal = 13) double inWorldY2,
										  @Local(ordinal = 14) double inWorldZ2
	) {
		return MorphMath.distanceTo(original, inWorldX1, inWorldY1, inWorldZ1, inWorldX2, inWorldY2, inWorldZ2, true, 0);
	}

	@ModifyVariable(method = "getWorldCoordinates", at = @At("HEAD"), remap = false)
	private static BlockPos normalizePos(BlockPos pos) {
		return InPlayerBlockPos.checkOnReal(pos);
	}

	@ModifyVariable(method = "getWorldCoordinates", at = @At("HEAD"), remap = false)
	private static Vector3d normalizePos(Vector3d vec) {
		if (!InPlayerBlockPos.isMorphedPlayerX(vec.x)) return vec;
		Vec3 vec3 = InPlayerBlockPos.checkOnReal(new Vec3(vec.x, vec.y, vec.z));
		return new Vector3d(vec3.x, vec3.y, vec3.z);
	}

}
