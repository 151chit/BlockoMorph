package net.blockomorph.mixins.compat.vs2;

import net.blockomorph.utils.hit.PlayerHitResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Predicate;


@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.world.RaycastUtilsKt", remap = false)
public class VS2_BM_RaycastUtil {

	@Inject(method = "clipIncludeShips", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
	private static void modifyClipIncludeShips(Level level, ClipContext ctx, boolean shouldTransformHitPos, Long skipShip, CallbackInfoReturnable<BlockHitResult> cir) {
		PlayerHitResult.checkHitResult(cir.getReturnValue().getLocation(), ctx, cir::setReturnValue);
	}

	@Inject(method = "clipIncludeShips", at = @At(value = "RETURN", ordinal = 1), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
	private static void modifyClipIncludeShipsWith(Level level, ClipContext ctx, boolean shouldTransformHitPos, Long skipShip, CallbackInfoReturnable<BlockHitResult> cir,
												   BlockHitResult vanillaHit, BlockHitResult closestHit, Vec3 closestHitPos
	) {
		PlayerHitResult.checkHitResult(closestHitPos, ctx, cir::setReturnValue);
	}

	@ModifyVariable(method = "raytraceEntities", at = @At("HEAD"))
	private static Predicate<Entity> eraseMorphedPlayerDouble(Predicate<Entity> original) {
		return original.and(PlayerHitResult.NOT_MORPHED_PLAYER);
	}
}
