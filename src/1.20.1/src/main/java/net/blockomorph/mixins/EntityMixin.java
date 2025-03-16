package net.blockomorph.mixins;

import net.blockomorph.utils.tnt.TntSpawnLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	private Level level;

	@Shadow public abstract AABB getBoundingBox();

	@Shadow public abstract void setPos(double p_20210_, double p_20211_, double p_20212_);

	@Shadow public abstract double getX();

	@Shadow public abstract double getY();

	@Shadow public abstract double getZ();

	@Inject(method = "level", at = @At("HEAD"), cancellable = true)
	public void level(CallbackInfoReturnable<Level> cir) {
		if (this.level instanceof TntSpawnLevel lv) {
			this.level = lv.getRealLevel();
		}
	}

	@Inject(method = "checkInsideBlocks", at = @At("HEAD"), cancellable = true)
	protected void level(CallbackInfo ci) {
		/*Entity self = (Entity)(Object)this;
		List<Entity> list = this.level.getEntities(self, this.getBoundingBox().inflate(1.0E-7D), EntitySelector.NO_SPECTATORS.and(entity -> (entity instanceof PlayerAccessor pl && pl.isFullActive())));
		for (Entity entity : list) {
			try {
				PlayerAccessor activedPlayer = (PlayerAccessor) entity;
				HashMap<BlockPos, BlockState> blocks = activedPlayer.getBlocks();
				blocks.put(BlockPos.ZERO, activedPlayer.getBlockState());
				for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
					AABB block = new AABB(entry.getKey());
					block = block.move(entity.position());
					block = block.move(-0.5, 0 , -0.5);
					if (this.getBoundingBox().intersects(block)) {
						//entry.getValue().entityInside(, self);
					}
				}
			} catch (Exception ignored) {
			}
		}*/
	}

	/*@ModifyConstant(method = "move", constant = @Constant(doubleValue = 1.0E-7D, ordinal = 1))
	public double fix(double constant) {
		return Double.MAX_VALUE;
	}*/

	/*@Inject(method = "move",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;lengthSqr()D", ordinal = 1, shift = At.Shift.BEFORE), // Найти точку, ГДЕ ВЫЗЫВАЕТСЯ ДЛИНА ПОСЛЕ COLLIDE, но ПЕРЕД ИСПОЛЬЗОВАНИЕМ vec3
			locals = LocalCapture.CAPTURE_FAILHARD,
			cancellable = true)
	private void fix2(MoverType p_19973_, Vec3 p_19974_, CallbackInfo ci, Vec3 vec3) {
		if (vec3.lengthSqr() > 1.0E-7D) {
			this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
		}
	}*/

}
