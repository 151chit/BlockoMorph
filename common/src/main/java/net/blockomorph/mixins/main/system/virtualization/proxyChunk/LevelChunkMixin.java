package net.blockomorph.mixins.main.system.virtualization.proxyChunk;

import net.blockomorph.core.coords.proxyChunk.PlayerConnectingSource;
import net.blockomorph.core.coords.proxyChunk.ProxyPlayerChunkHandler;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("Invalid_FI_return_type")
@Mixin(LevelChunk.class)
public class LevelChunkMixin {
	@Shadow @Final private Level level;

	@FastInject(method = "setBlockState", at = @At(value = "HEAD"))
	public Object setBlock(BlockPos pos, BlockState state, int flags) {
		return ProxyPlayerChunkHandler.setBlockState(PlayerConnectingSource.AUTOMATIC, pos, state, flags);
	}

	@FastInject(method = "removeBlockEntity", at = @At(value = "HEAD"))
	public boolean removeEntity(BlockPos pos) {
		return ProxyPlayerChunkHandler.removeBlockEntity(PlayerConnectingSource.AUTOMATIC, pos);
	}

	@FastInject(method = "setBlockEntity", at = @At(value = "HEAD"))
	public boolean setBlockEntity(BlockEntity blockEntity) {
		return ProxyPlayerChunkHandler.changeBlockEntity(PlayerConnectingSource.AUTOMATIC, blockEntity, false);
	}

	@FastInject(method = "addAndRegisterBlockEntity", at = @At("HEAD"))
	public boolean registerBlockEntity(BlockEntity blockEntity) {
		return ProxyPlayerChunkHandler.changeBlockEntity(PlayerConnectingSource.AUTOMATIC, blockEntity, true);
	}

	@FastInject(method = "getBlockState", at = @At(value = "HEAD"))
	public Object getBlock(BlockPos pos) {
		return ProxyPlayerChunkHandler.getBlockState(PlayerConnectingSource.AUTOMATIC, this.level, pos);
	}

	@FastInject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At(value = "HEAD"))
	public Object getEntity(BlockPos pos, LevelChunk.EntityCreationType creationType) {
		return ProxyPlayerChunkHandler.getBlockEntity(PlayerConnectingSource.AUTOMATIC, this.level, pos);
	}

	@FastInject(method = "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", at = @At(value = "HEAD"))
	public Object getFluid(int x, int y, int z) {
		return ProxyPlayerChunkHandler.getFluidState(PlayerConnectingSource.AUTOMATIC, this.level, x, y, z);
	}
}
