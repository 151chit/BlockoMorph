package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.accessors.MovementAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.world.entity.Entity$Movement")
public class MovementMixin implements MovementAccessor {

	@Shadow @Final private Vec3 from;

	@Shadow @Final Vec3 to;

	@Override
	public Vec3[] getMovement$blockomorph() {
		Vec3[] movements = new Vec3[2];
		movements[0] = this.from;
		movements[1] = this.to;
		return movements;
	}
}
