package net.blockomorph.mixins.main.system.virtualization.normalize.level;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerLevel.class)
public class ServerLevelExplodeMixin {

	@ModifyVariable(method = "explode", at = @At("STORE"))
	private Vec3 norm(Vec3 center) {
		return MorphNormalizer.normalize(center);
	}
}
