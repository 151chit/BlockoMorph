package net.blockomorph.mixins.compat.vs2;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt", remap = false)
public abstract class VS2_BM_VsUtilsMixin {

	@Inject(method = "squaredDistanceBetweenInclShips(Lnet/minecraft/world/level/Level;DDDDDD)D", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
	private static void normalizeCoords(Level lv, double x, double y, double z, double x2, double y2, double z2, CallbackInfoReturnable<Double> cir,
										double dx, double dy, double dz, double x3, double y3, double z3, double x4, double y4, double z4
	) {
		MorphUtils.distanceTo(new Vec3(x3, y3, z3), new Vec3(x4, y4, z4), true, 0, cir::setReturnValue);
	}

	@ModifyVariable(method = "getWorldCoordinates", at = @At("HEAD"), remap = false)
	private static BlockPos normalizePos(BlockPos pos) {
		return InPlayerBlockPos.checkOnReal(pos);
	}

	@ModifyVariable(method = "getWorldCoordinates", at = @At("HEAD"), remap = false)
	private static Vector3d normalizePos(Vector3d vec) {
		Vec3 vec3 = InPlayerBlockPos.checkOnReal(new Vec3(vec.x, vec.y, vec.z));
		return new Vector3d(vec3.x, vec3.y, vec3.z);
	}

}
