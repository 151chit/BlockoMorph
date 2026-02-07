package net.blockomorph.mixins.fabric;

import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class BlockDestroySupportMixin {

	@Inject(method = "addBreakingBlockEffect", at = @At(value = "HEAD"), cancellable = true)
	public void crack(BlockPos pos, Direction dir, CallbackInfo ci) {
		ClientPlatformUtils.crackBlock((ClientLevel) (Object)this, pos, dir, BlockBehaviour.BlockStateBase::shouldSpawnTerrainParticles, ci);
	}
}
