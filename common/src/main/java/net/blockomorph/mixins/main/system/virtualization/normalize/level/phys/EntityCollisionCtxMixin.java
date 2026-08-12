package net.blockomorph.mixins.main.system.virtualization.normalize.level.phys;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityCollisionContext.class)
public class EntityCollisionCtxMixin {
	@Shadow @Final @Nullable private Entity entity;
	@Shadow @Final private double entityBottom;

	@FastInject(method = "isAbove", at = @At("HEAD"))
	public byte isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
		if (this.entity != null) {
			PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
			if (pl != null) {
				int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
				if (posIn != -1) {
					boolean result = this.entityBottom >
							MorphMath.getRealBlockPosAxis(Direction.Axis.Y, pl, InPlayerBlockPos.getY(posIn)) + shape.max(Direction.Axis.Y) - (double) 1.0E-5F;
					return (byte) (result ? 1 : -1);
				}
			}
		}
		return 0;
	}

}
