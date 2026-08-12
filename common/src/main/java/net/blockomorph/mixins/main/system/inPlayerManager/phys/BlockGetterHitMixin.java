package net.blockomorph.mixins.main.system.inPlayerManager.phys;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.phys.hit.PlayerHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockGetter.class) //todo: vs2
public interface BlockGetterHitMixin {

	@ModifyReturnValue(method = "clip", at = @At("RETURN"))
	private BlockHitResult hitMorphs(BlockHitResult original, ClipContext ctx) {
		var morphHit = PlayerHitResult.clip(this, ctx);
		if (morphHit != null) {
			if (original == null || original.getType() == HitResult.Type.MISS) return morphHit;
			Vec3 from = ctx.getFrom();
			if (morphHit.distanceToRealSqr(from) <= original.getLocation().distanceToSqr(from))
				return morphHit;
		}
		return original;
	}

	@ModifyReturnValue(method = "isBlockInLine", at = @At("RETURN"))
	private BlockHitResult hitMorphsInLine(BlockHitResult original, ClipBlockStateContext ctx) {
		if (original == null || original.getType() == HitResult.Type.MISS) {
			var morphHit = PlayerHitResult.clipBlockInLine(this, ctx);
			if (morphHit != null)
				return morphHit;
		}
		return original;
	}
}
