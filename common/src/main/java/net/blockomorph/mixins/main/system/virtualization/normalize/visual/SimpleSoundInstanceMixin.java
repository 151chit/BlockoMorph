package net.blockomorph.mixins.main.system.virtualization.normalize.visual;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleSoundInstance.class)
public abstract class SimpleSoundInstanceMixin extends AbstractSoundInstance {

	protected SimpleSoundInstanceMixin(SoundEvent event, SoundSource source, RandomSource random) {
		super(event, source, random);
	}

	@Inject(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/util/RandomSource;ZILnet/minecraft/client/resources/sounds/SoundInstance$Attenuation;DDDZ)V", at = @At(value = "TAIL"))
	public void init(CallbackInfo ci,
					 @Local(ordinal = 0, argsOnly = true) double x,
					 @Local(ordinal = 1, argsOnly = true) double y,
					 @Local(ordinal = 2, argsOnly = true) double z) {
		this.x = MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.X);
		this.y = MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Y);
		this.z = MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Z);
	}
}
