package net.blockomorph.mixins.main.system.virtualization.normalize.visual;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

	@Shadow protected ClientLevel level;

	@Shadow @Final private RandomSource random;

	@Inject(method = "add", at = @At("HEAD"))
	private void norm(Particle p, CallbackInfo ci) {
		MorphNormalizer.normalizeParticlePos(p);
	}

	@FastInject(method = "makeParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleProvider;createParticle(Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/util/RandomSource;)Lnet/minecraft/client/particle/Particle;"))
	private <T extends ParticleOptions> Object norm(T options, double x, double y, double z, double xa, double ya, double za, @Local ParticleProvider<T> provider) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) {
			return provider.createParticle(options, this.level,
					MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.X),
					MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Y),
					MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Z), xa, ya, za, this.random);
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}
