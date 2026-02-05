package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModelDataManager.class, remap = false)
public abstract class ModelDataManagerMixin {
	@Shadow @Final private Level level;

	@Shadow protected abstract boolean isOtherThread();

	@Inject(method = "requestRefresh", at = @At(value = "HEAD"), cancellable = true)
	public void redirect(BlockEntity blockEntity, CallbackInfo ci) {
		if (this.isOtherThread()) return;
		InPlayerBlockPos.check(blockEntity.getBlockPos(), (pl, realPos) -> {
			ci.cancel();
			BlockInPlayer2 block = pl.getBlocksData2().get(realPos);
			if (block != null) {
				block.connectAdditionalRenderData(blockEntity.getModelData());
			}
		}, ci::cancel, this.level);
	}

	@Inject(method = "getAt(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/model/data/ModelData;", at = @At("HEAD"), cancellable = true)
	private void returnBlock(BlockPos pos, CallbackInfoReturnable<ModelData> cir) {
		if (this.isOtherThread()) return;
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			cir.setReturnValue(ModelData.EMPTY);
			BlockInPlayer2 block = pl.getBlocksData2().get(realPos);
			if (block != null && block.getModelData() instanceof ModelData data) {
				cir.setReturnValue(data);
			}
		}, () -> cir.setReturnValue(ModelData.EMPTY), this.level);
	}
}