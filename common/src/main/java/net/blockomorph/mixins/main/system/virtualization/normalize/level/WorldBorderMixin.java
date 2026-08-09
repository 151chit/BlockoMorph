package net.blockomorph.mixins.main.system.virtualization.normalize.level;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldBorder.class)
public class WorldBorderMixin {

	@ModifyReturnValue(method = "isWithinBounds(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("RETURN"))
	private boolean check(boolean original, ChunkPos pos) {
		if (InPlayerBlockPos.isMorphedPlayerChunkX(pos.x)) return true;
		return original;
	}

	@ModifyReturnValue(method = "isWithinBounds(DDD)Z", at = @At("RETURN"))
	private boolean checkMargin(boolean original, double x) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(x)) return true;
		return original;
	}

	@ModifyReturnValue(method = "isWithinBounds(Lnet/minecraft/world/phys/AABB;)Z", at = @At("RETURN"))
	private boolean check(boolean original, AABB aabb) {
		if (InPlayerBlockPos.isMorphedPlayerBlockX(aabb.minX) || InPlayerBlockPos.isMorphedPlayerBlockX(aabb.maxX) ||
				InPlayerBlockPos.isMorphedPlayerBlockX(Mth.lerp(0.5, aabb.minX, aabb.maxX))) return true;
		return original;
	}
}
