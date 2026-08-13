package net.blockomorph.mixins.main.system.virtualization.normalize.lockExternal;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(NeighborUpdater.class)
public interface ShapeUpdateTypeMixin {

	@ModifyVariable(method = "executeShapeUpdate", at = @At("STORE"), ordinal = 1)
	private static BlockState disableWorldAutoread(BlockState orig, LevelAccessor levelAccessor, Direction direction, BlockPos blockPos) {
		return MorphUtils.lockExternalMorphedGetter(orig, blockPos);
	}

	@ModifyVariable(method = "executeUpdate", at = @At("HEAD"), ordinal = 0)
	private static BlockState disableWorldAutoreadUpdate(BlockState blockState, Level level, BlockState blockState2, BlockPos pos) {
		return MorphUtils.lockExternalMorphedGetter(blockState, pos);
	}
}