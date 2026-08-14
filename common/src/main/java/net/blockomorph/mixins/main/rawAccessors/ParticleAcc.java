package net.blockomorph.mixins.main.rawAccessors;

import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public class ParticleAcc implements Accessors.ParticleAccessor {
	@Shadow protected double x;
	@Shadow protected double y;
	@Shadow protected double z;

	@Override
	public double getCoords$bm(Direction.Axis axis) {
		return switch (axis) {
			case X -> this.x;
			case Y -> this.y;
			case Z -> this.z;
		};
	}
}
