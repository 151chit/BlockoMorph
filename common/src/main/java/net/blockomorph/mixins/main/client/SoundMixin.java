package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleSoundInstance.class)
public class SoundMixin extends AbstractSoundInstance {

	protected SoundMixin(SoundEvent p_235072_, SoundSource p_235073_, RandomSource p_235074_) {
		super(p_235072_, p_235073_, p_235074_);
		throw new AssertionError();
	}

	@Inject(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/util/RandomSource;ZILnet/minecraft/client/resources/sounds/SoundInstance$Attenuation;DDDZ)V", at = @At(value = "TAIL"))
	public void init(ResourceLocation location, SoundSource source, float volume, float pitch, RandomSource random, boolean looping, int delay, Attenuation attenuation, double x, double y, double z, boolean relative, CallbackInfo ci) {
		Vec3 vec = InPlayerBlockPos.checkOnReal(new Vec3(x, y, z));
		this.x = vec.x;
		this.y = vec.y;
		this.z = vec.z;
	}
}
