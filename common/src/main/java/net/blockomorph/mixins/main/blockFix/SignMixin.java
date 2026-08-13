package net.blockomorph.mixins.main.blockFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignBlockEntity.class)
public abstract class SignMixin extends BlockEntity {

	public SignMixin(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
		super(type, worldPosition, blockState);
	}

	@FastInject(method = "isFacingFrontText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D"))
	public byte check(Player player, @Local Vec3 vec3, @Local SignBlock signBlock) {
		BlockPos pos = this.getBlockPos();
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				double d0 = player.getX() - (MorphMath.getRealBlockPosAxis(Direction.Axis.X, pl, InPlayerBlockPos.getX(posIn)) + vec3.x);
				double d1 = player.getZ() - (MorphMath.getRealBlockPosAxis(Direction.Axis.Z, pl, InPlayerBlockPos.getZ(posIn)) + vec3.z);
				float f = signBlock.getYRotationDegrees(this.getBlockState());
				float f1 = (float) (Mth.atan2(d1, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
				return (byte) (Mth.degreesDifferenceAbs(f, f1) <= 90.0F ? 1 : -1);
			}
		}
		return 0;
	}
}
