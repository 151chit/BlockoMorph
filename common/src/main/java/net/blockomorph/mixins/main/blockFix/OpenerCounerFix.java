package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ContainerOpenersCounter.class)
public class OpenerCounerFix {

	@ModifyVariable(method = "getOpenCount", at = @At("STORE"), ordinal = 0)
	private AABB inflate(AABB original, Level lv, BlockPos pos) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
			return original.inflate(8); //part of vannila bug:
			//When moving away from the container, it closes with a visual error, the transformed player will run, so the error will appear often
		}
		return original;
	}
}
