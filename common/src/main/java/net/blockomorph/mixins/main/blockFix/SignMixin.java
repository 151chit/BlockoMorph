package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignBlockEntity.class)
public class SignMixin extends BlockEntity {

	public SignMixin() {
		super(null, null, null);
	}

	@Inject(method = "isFacingFrontText", at = @At("HEAD"), cancellable = true)
	public void check(Player player, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(this.getBlockPos(), (pl, realPos) -> {
			if (this.getBlockState().getBlock() instanceof SignBlock signblock) {
				Vec3 vec3 = signblock.getSignHitboxCenterPosition(this.getBlockState());
				Vec3 real = MorphUtils.getRealBlockPos(pl, realPos);
				double d0 = player.getX() - (real.x + vec3.x);
				double d1 = player.getZ() - (real.z + vec3.z);
				float f = signblock.getYRotationDegrees(this.getBlockState());
				float f1 = (float) (Mth.atan2(d1, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
				cir.setReturnValue(Mth.degreesDifferenceAbs(f, f1) <= 90.0F);
			}
		}, null, player.level().isClientSide);
	}
}
