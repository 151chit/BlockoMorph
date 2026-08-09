package net.blockomorph.utils.config.enums;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public enum PlaceMode { DISABLED, IN, OUT;

	public static UseOnContext getRealWorldPosIfOutPlaceMode(UseOnContext ctx) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(ctx.getClickedPos().getX()) && ctx.getItemInHand().getItem() instanceof BlockItem) {
			if (Config.get().placeMode.getValue() == OUT) {
				Vec3 realHit = MorphNormalizer.normalize(ctx.getClickLocation());
				realHit = toDirection(realHit, ctx.getClickedFace());
				BlockHitResult hit = new BlockHitResult(realHit, ctx.getClickedFace(), BlockPos.containing(realHit), ctx.isInside());
				return new UseOnContext(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getItemInHand(), hit) {};
			}
		}
		return ctx;
	}

	public static void rejectClickIfNoneValidPlaceMode(Player serverPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(blockHitResult.getBlockPos().getX()) && serverPlayer.getItemInHand(interactionHand).getItem() instanceof BlockItem) {
			if (Config.get().placeMode.getValue() == DISABLED) {
				cir.setReturnValue(InteractionResult.PASS);
			}
		}
	}

	private static Vec3 toDirection(Vec3 vec, Direction dir) {
		Vec3i step = dir.getUnitVec3i();
		double x = switch (step.getX()) {
			case 1 -> Math.ceil(vec.x) + 1.0E-7;
			case -1 -> Math.floor(vec.x) - 1.0E-7;
			default -> vec.x;
		};
		double y = switch (step.getY()) {
			case 1 -> Math.ceil(vec.y) + 1.0E-7;
			case -1 -> Math.floor(vec.y) - 1.0E-7;
			default -> vec.y;
		};
		double z = switch (step.getZ()) {
			case 1 -> Math.ceil(vec.z) + 1.0E-7;
			case -1 -> Math.floor(vec.z) - 1.0E-7;
			default -> vec.z;
		};
		return new Vec3(x, y, z);
	}
}
