package net.blockomorph.mixins.main.system.virtualization.normalize.visual;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.client.particle.VibrationSignalParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(VibrationSignalParticle.class)
public class VibrationParticleMixin {

	@ModifyVariable(method = "<init>", at = @At("STORE"))
	private Optional<Vec3> normalizeSource(Optional<Vec3> position) {
		return position.map(MorphNormalizer::normalize);
	}

	@ModifyVariable(method = "tick", at = @At("STORE"))
	private Optional<Vec3> normalizeSourceTick(Optional<Vec3> position) {
		return position.map(MorphNormalizer::normalize);
	}
}
