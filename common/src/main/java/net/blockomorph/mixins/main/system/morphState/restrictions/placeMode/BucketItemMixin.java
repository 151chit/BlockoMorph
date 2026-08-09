package net.blockomorph.mixins.main.system.morphState.restrictions.placeMode;

import net.blockomorph.utils.config.Config;
import net.blockomorph.core.phys.hit.MorphedPlayerHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BucketItem.class)
public class BucketItemMixin {
	@Shadow @Final private Fluid content;

	@ModifyVariable(method = "use", at = @At("STORE"))
	public BlockHitResult placeOut(BlockHitResult hitResult) {
		if (hitResult instanceof MorphedPlayerHitResult playerHitResult && this.content != Fluids.EMPTY) {
			Vec3 vec = playerHitResult.getRealPos();
			Vec3 tolerance = Vec3.atLowerCornerOf(playerHitResult.getDirection().getUnitVec3i()).scale(-1.0E-7);
			BlockPos pos = BlockPos.containing(vec.add(tolerance));
			return switch (Config.get().placeMode.getValue()) {
				case OUT -> new BlockHitResult(vec, playerHitResult.getDirection(), pos, playerHitResult.isInside());
				case DISABLED -> BlockHitResult.miss(vec, playerHitResult.getDirection(), pos);
				case IN -> hitResult;
			};
		}
		return hitResult;
	}
}