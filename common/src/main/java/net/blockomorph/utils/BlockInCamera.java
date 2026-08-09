package net.blockomorph.utils;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.storage.playerSection.PlayersStorage;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class BlockInCamera {

	public static BlockInPlayer2 findFluid(Camera camera, BlockGetter getter) {
		Camera.NearPlane nearPlane = camera.getNearPlane();
		for (Vec3 pos : Arrays.asList(nearPlane.getPointOnPlane(0, 0), nearPlane.getTopLeft(), nearPlane.getTopRight(), nearPlane.getBottomLeft(), nearPlane.getBottomRight())) {
			BlockInPlayer2 block = PlayersStorage.ofLevel(getter).findFirstBlockOnPos(skipSelfProtectionIfLocalPlayerF5(camera.entity()),
					camera.position().x + pos.x, camera.position().y + pos.y, camera.position().z + pos.z,
					(testBlock, ignored, ignoredX, y, ignoredZ) -> {
						if (testBlock.shouldDoFluidAction()) {
							FluidState fluidState = testBlock.getBlockState().getFluidState();
							double blockPosY = MorphMath.getRealBlockPosAxis(Direction.Axis.Y, testBlock);
							double height = fluidState.getHeight(testBlock.getOwner().level(), testBlock.getPos());
							return blockPosY + height > y;
						}
						return false;
					});
			if (block != null) return block;
		}
		return null;
	}

	public static BlockInPlayer2 findBlock(Camera camera, BlockGetter getter) {
		Camera.NearPlane nearPlane = camera.getNearPlane();
		for (Vec3 pos : Arrays.asList(nearPlane.getPointOnPlane(0, 0), nearPlane.getTopLeft(), nearPlane.getTopRight(), nearPlane.getBottomLeft(), nearPlane.getBottomRight())) {
			BlockInPlayer2 block = PlayersStorage.ofLevel(getter).findFirstBlockOnPos(skipSelfProtectionIfLocalPlayerF5(camera.entity()),
					camera.position().x + pos.x, camera.position().y + pos.y, camera.position().z + pos.z,
					(testBlock, ignored, ignoredX, ignoredY, ignoredZ) -> {
						BlockState blockState = testBlock.getBlockState();
						return blockState.getRenderShape() != RenderShape.INVISIBLE &&
								blockState.isViewBlocking(testBlock.getOwner().level(), testBlock.getPos());
					});
			if (block != null) return block;
		}
		return null;
	}

	public static Entity skipSelfProtectionIfLocalPlayerF5(Entity entity) {
		if (entity == GuiUtils.MC.player && !GuiUtils.MC.options.getCameraType().isFirstPerson()) {
			return null;
		}
		return entity;
	}
}
