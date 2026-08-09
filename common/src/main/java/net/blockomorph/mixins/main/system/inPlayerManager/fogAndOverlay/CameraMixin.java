package net.blockomorph.mixins.main.system.inPlayerManager.fogAndOverlay;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.utils.BlockInCamera;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow private Level level;
	@Shadow private Entity entity;

	@FastInject(method = "getFluidInCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"))
	public Object getVanillaFluid(@Local(ordinal = 1) Vec3 offsetPos) {
		PlayersStorage storage = PlayersStorage.ofLevel(this.level);
		try (storage) {
			for (Player playerOwner : storage.findMorphedOnPos(BlockInCamera.skipSelfProtectionIfLocalPlayerF5(this.entity), offsetPos.x, offsetPos.y, offsetPos.z)) {
				BlockInPlayer2 block = MorphMath.getBlockInPlayerOnPos(PlayerAccessor.of(playerOwner), offsetPos.x, offsetPos.y, offsetPos.z);
				if (block != null) {
					if (block.getBlockState().is(Blocks.POWDER_SNOW)) {
						return FogType.POWDER_SNOW;
					} else if (block.shouldDoFluidAction()) {
						FluidState fluidState = block.getBlockState().getFluidState();
						if (offsetPos.y < MorphMath.getRealBlockPosAxis(Direction.Axis.Y, block.getPlayer(), block.getOffset().y) +
								fluidState.getHeight(this.level, block.getPos())) {
							if (fluidState.is(FluidTags.WATER)) {
								return FogType.WATER;
							} else if (fluidState.is(FluidTags.LAVA)) {
								return FogType.LAVA;
							}
						}
					}
				}
			}
		}
		return FastInject.CONTINUE_EXECUTION;
	}
}