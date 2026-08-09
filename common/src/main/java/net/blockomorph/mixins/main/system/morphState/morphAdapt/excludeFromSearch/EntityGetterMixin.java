package net.blockomorph.mixins.main.system.morphState.morphAdapt.excludeFromSearch;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityGetter.class)
public interface EntityGetterMixin {

	@ModifyVariable(method = "getNearestPlayer(DDDDLjava/util/function/Predicate;)Lnet/minecraft/world/entity/player/Player;", at = @At(value = "STORE"), ordinal = 5)
	private double removeSelf(double dist, double x, double y, double z) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(Mth.floor(x), Mth.floor(z));
		if (pl != null) {
			return Double.MAX_VALUE;
		}
		return dist;
	}
}
