package net.blockomorph.mixins.main.system.morphState.morphAdapt.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(DragonEggBlock.class)
public class DragonEggMixin {

	@FastInject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean tpPlayer(BlockState state, Level level, BlockPos pos, @Local(ordinal = 1) BlockPos testPos) {
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl instanceof ServerPlayer player) {
			double x = MorphNormalizer.normalize(Direction.Axis.X, testPos.getX() + 0.5, pos.getX(), pos.getZ());
			double y = MorphNormalizer.normalize(Direction.Axis.Y, testPos.getY() + 0.5, pos.getX(), pos.getZ());
			double z = MorphNormalizer.normalize(Direction.Axis.Z, testPos.getZ() + 0.5, pos.getX(), pos.getZ());
			player.teleportTo(player.level(), x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
			return false;
		}
		return true;
	}//Particles don't fly where the egg does because randomness is broken in vanilla?!
}
