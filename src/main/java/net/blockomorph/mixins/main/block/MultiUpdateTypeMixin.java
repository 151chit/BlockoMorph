package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(targets = "net.minecraft.world.level.redstone.CollectingNeighborUpdater$MultiNeighborUpdate")
public class MultiUpdateTypeMixin {

	@ModifyVariable(method = "runNext", at = @At("STORE"))
	public BlockPos changePos(BlockPos orig, Level lv) {
		AtomicReference<BlockPos> value = new AtomicReference<>(orig);
		InPlayerBlockPos.check(orig, (pl, realPos) -> {
			if (!pl.getBlocksData2().containsKey(realPos)) {
				value.set(InPlayerBlockPos.checkOnReal(orig));
			}
		}, null, lv);
		return value.get();
	}
}
