package net.blockomorph.mixins.neoforge;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ModelDataManager.class, remap = false)
public abstract class ModelDataManagerMixin {
	@Shadow protected abstract boolean isOtherThread();

	@FastInject(method = "requestRefresh", at = @At(value = "HEAD"))
	public boolean redirect(BlockEntity blockEntity) {
		return !InPlayerBlockPos.isMorphedPlayerBlockX(blockEntity.getBlockPos().getX());//direct read instead
	}

	@FastInject(method = "getAt(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/model/data/ModelData;", at = @At("HEAD"))
	private Object returnBlock(BlockPos pos) {
		if (this.isOtherThread()) return FastInject.CONTINUE_EXECUTION;
		PlayerAccessor pl = InPlayerBlockPos.findPlayer(pos);
		if (pl != null) {
			int posIn = InPlayerBlockPos.findInPlayerBlockPos(pos);
			if (posIn != -1) {
				BlockInPlayer2 block = pl.getBlock(posIn);
				if (block != null && block.getBlockEntity() != null)
					return block.getBlockEntity().getModelData();
			}
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}