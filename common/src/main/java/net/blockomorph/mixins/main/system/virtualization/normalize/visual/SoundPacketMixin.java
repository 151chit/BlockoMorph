package net.blockomorph.mixins.main.system.virtualization.normalize.visual;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSoundPacket.class)
public class SoundPacketMixin {
	@Mutable @Shadow @Final private int x;
	@Mutable @Shadow @Final private int y;
	@Mutable @Shadow @Final private int z;

	@Inject(method = "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;DDDFFJ)V", at = @At("RETURN"))
	private void norm(Holder<?> sound, SoundSource source, double x, double y, double z, float volume, float pitch, long seed, CallbackInfo ci) {
		this.x = (int) (MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.X) * 8);
		this.y = (int) (MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Y) * 8);
		this.z = (int) (MorphNormalizer.normalizeOneOf(x, y, z, Direction.Axis.Z) * 8);
	}
}
