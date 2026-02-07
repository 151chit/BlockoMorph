package net.blockomorph.screens.utils;

import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.platform.ClientPlatformUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public final class FogLiquidModifier {
	public record LiquidFogData(float start, float end, int color) {
	}

	@Nullable
	public LiquidFogData getFog(Iterable<Entity> entities, boolean includeVanilla) {
		AtomicReference<LiquidFogData> fogData = new AtomicReference<>();

		Camera camera = GuiUtils.MC.gameRenderer.getMainCamera();
		Camera.NearPlane nearPlane = camera.getNearPlane();
		for (Vec3 pos : Arrays.asList(nearPlane.getPointOnPlane(0, 0), nearPlane.getTopLeft(), nearPlane.getTopRight(), nearPlane.getBottomLeft(), nearPlane.getBottomRight())) {
			Vec3 position = camera.position().add(pos);
			if (fogData.get() != null) break;
			MorphUtils.doBlockInMorphedPlayerOnPos(camera.entity(), entities, position, (pl, block) -> {
				if (block.shouldDoFluidAction()) {
					FluidState fluidState = block.getBlockState().getFluidState();
					if (includeVanilla || (!fluidState.getType().isSame(Fluids.WATER) && !fluidState.getType().isSame(Fluids.LAVA))) {
						double y = MorphUtils.getRealBlockPos(pl, block.getOffset()).y;
						double height = fluidState.getHeight(camera.entity().level(), block.getPos());
						if (y + height > position.y) {
							fogData.set(ClientPlatformUtils.INSTANCE.calculateData(camera.entity().level(), block));
						}
					}
				}
			});
		}
		return fogData.get();
	}

	@Nullable
	public static TextureAtlasSprite[] getPlatformFluidSprite(BlockAndTintGetter lv, BlockState state, BlockPos keyPos) {
		return ClientPlatformUtils.INSTANCE.getPlatformFluidSprite(lv, state, keyPos);
	}
	public static @NotNull Integer getPlatformFluidTint(BlockAndTintGetter lv, BlockState state, BlockPos keyPos) {
		return ClientPlatformUtils.INSTANCE.getPlatformFluidTint(lv, state, keyPos);
	}
}
