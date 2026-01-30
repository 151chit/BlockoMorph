package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.coords.DummyLevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
	@Shadow @Final
	Level level;

	@Inject(method = "setBlockState", at = @At(value = "HEAD"), cancellable = true)
	public void setBlock(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
		DummyLevelChunk.setBlockState(this.level, pos, state, flags, cir);
	}

	@Inject(method = "removeBlockEntity", at = @At(value = "HEAD"))
	public void removeEntity(BlockPos pos, CallbackInfo ci) {
		DummyLevelChunk.removeBlockEntity(this.level, pos, ci);
	}

	@Inject(method = "setBlockEntity", at = @At(value = "HEAD"))
	public void setBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
		DummyLevelChunk.changeBlockEntity(this.level, blockEntity, false, ci);
	}

	@Inject(method = "addAndRegisterBlockEntity", at = @At("HEAD"))
	public void registerBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
		DummyLevelChunk.changeBlockEntity(this.level, blockEntity, true, ci);
	}

	@Inject(method = "getBlockState", at = @At(value = "HEAD"), cancellable = true)
	public void getBlock(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		DummyLevelChunk.getBlockState(this.level, pos, cir);
	}

	@Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;", at = @At(value = "HEAD"), cancellable = true)
	public void getEntity(BlockPos pos, LevelChunk.EntityCreationType p_62869_, CallbackInfoReturnable<BlockEntity> cir) {
		DummyLevelChunk.getBlockEntity(this.level, pos, cir);
	}

	@Inject(method = "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", at = @At(value = "HEAD"), cancellable = true)
	public void getFluid(int x, int y, int z, CallbackInfoReturnable<FluidState> cir) {
		DummyLevelChunk.getFluidState(this.level, new BlockPos(x, y, z), cir);
	}
}
