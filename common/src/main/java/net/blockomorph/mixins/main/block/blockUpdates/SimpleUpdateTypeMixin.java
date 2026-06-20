package net.blockomorph.mixins.main.block.blockUpdates;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.world.level.redstone.CollectingNeighborUpdater$SimpleNeighborUpdate")
public class SimpleUpdateTypeMixin {
	@Mutable @Final @Shadow private BlockPos pos;
	@Shadow @Final private BlockPos neighborPos;
	@Shadow @Final private Block block;

	@ModifyVariable(method = "runNext", at = @At("STORE"))
	private BlockState disableWorldAutoread(BlockState orig, Level lv) {
		this.pos = MorphUtils.normalizeToRealOnOutline(this.pos, lv);
		return MorphUtils.lockExternalMorphedGetter(orig, lv, this.pos);
	}

	@ModifyReturnValue(method = "runNext", at = @At("RETURN"))
	private boolean checkPlayers(boolean original, Level lv) {
		MorphUtils.checkMorphs(lv, this.pos, this.neighborPos, this.block);
		return original;
	}
}
