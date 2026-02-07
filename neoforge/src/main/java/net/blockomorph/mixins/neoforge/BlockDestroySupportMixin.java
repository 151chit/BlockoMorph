package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class BlockDestroySupportMixin {
	@Shadow @Final private Minecraft minecraft;

	@Inject(method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/HitResult;)V", at = @At(value = "HEAD"), cancellable = true)
	public void crack(BlockPos pos, Direction dir, @Nullable HitResult hitResult, CallbackInfo ci) {
		ClientLevel level = (ClientLevel) (Object)this;
		ClientPlatformUtils.crackBlock(level, pos, dir, blockState -> {
			return !IClientBlockExtensions.of(blockState.getBlock()).addHitEffects(blockState, level, hitResult, this.minecraft.particleEngine);
		}, ci);
	}
}
