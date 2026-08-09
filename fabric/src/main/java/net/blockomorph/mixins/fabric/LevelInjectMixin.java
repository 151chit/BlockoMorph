package net.blockomorph.mixins.fabric;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public class LevelInjectMixin {

	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "STORE"), ordinal = 2)
	public BlockState isolate(BlockState value, BlockPos pos) {
		return MorphUtils.lockExternalMorphedGetter(value, pos);
	}
}
