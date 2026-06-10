package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.particle.VibrationSignalParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(VibrationSignalParticle.class)
public class VibrationParticleMixin {

	@ModifyVariable(method = "<init>", at = @At("STORE"))
	private Optional<Vec3> normalizeSource(Optional<Vec3> orig) {
		return orig.map(InPlayerBlockPos::checkOnReal);
	}

	@ModifyVariable(method = "tick", at = @At("STORE"))
	private Optional<Vec3> normalizeSourceTick(Optional<Vec3> orig) {
		return orig.map(InPlayerBlockPos::checkOnReal);
	}
}
