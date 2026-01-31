package net.blockomorph.mixins.main.client.graphic;

import com.mojang.blaze3d.platform.Lighting;
import net.blockomorph.utils.accessors.LightningSetter;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lighting.class)
public abstract class LightningMixin implements LightningSetter {
	@Shadow
	public abstract void setupFor(Lighting.Entry entry);

	@Shadow
	protected abstract void updateBuffer(Lighting.Entry entry, Vector3f vector3f, Vector3f vector3f2);

	@Unique private Vector3f[] vanillaItems3Dlight;

	public void runWithLight(Runnable rendering, Vector3f start, Vector3f end) {
		this.setupFor(Lighting.Entry.ENTITY_IN_UI);
		this.updateBuffer(Lighting.Entry.ENTITY_IN_UI, start, end);
		rendering.run();
	}

	public void disable() {
		this.updateBuffer(Lighting.Entry.ENTITY_IN_UI, this.vanillaItems3Dlight[0], this.vanillaItems3Dlight[1]);
	}

	@Inject(method = "updateBuffer", at = @At("HEAD"))
	public void update(Lighting.Entry entry, Vector3f vector3f, Vector3f vector3f2, CallbackInfo ci) {
		if (entry == Lighting.Entry.ENTITY_IN_UI && this.vanillaItems3Dlight == null) {
			Vector3f[] vec = new Vector3f[2];
			vec[0] = vector3f;
			vec[1] = vector3f2;
			this.vanillaItems3Dlight = vec;
		}
	}
}
