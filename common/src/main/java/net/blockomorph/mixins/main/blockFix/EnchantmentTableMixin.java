package net.blockomorph.mixins.main.blockFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantingTableBlockEntity.class)
public class EnchantmentTableMixin {

	@Inject(method = "bookAnimationTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;tRot:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, ordinal = 0))
	private static void getRealBlockPos(Level level, BlockPos worldPosition, BlockState state, EnchantingTableBlockEntity entity, CallbackInfo ci, @Local Player player) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(worldPosition);
		if (pl != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(worldPosition);
			if (posIn != -1) {
				Vec3 vec3 = player.position();
				double x = vec3.x - (MorphMath.getRealBlockPosAxis(Direction.Axis.X, pl, InPlayerBlockPos.getX(posIn)) + 0.5);
				double z = vec3.z - (MorphMath.getRealBlockPosAxis(Direction.Axis.Z, pl, InPlayerBlockPos.getZ(posIn)) + 0.5);
				entity.tRot = (float) Mth.atan2(z, x);
			}
		}
	}

}
