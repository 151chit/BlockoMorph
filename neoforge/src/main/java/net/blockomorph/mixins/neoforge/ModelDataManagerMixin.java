package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelDataManager.class, remap = false)
public class ModelDataManagerMixin {
	@Shadow @Final private Level level;

	@Inject(method = "requestRefresh", at = @At(value = "HEAD"), cancellable = true)
	public void redirect(BlockEntity blockEntity, CallbackInfo ci) {
		InPlayerBlockPos.check(blockEntity.getBlockPos(), (pl, realPos) -> {
			ci.cancel();
			BlockInPlayer2 block = pl.getBlocksData2().get(realPos);
			if (block != null) {
				block.connectAdditionalRenderData(blockEntity.getModelData());
			}
		}, ci::cancel, this.level);
	}
}