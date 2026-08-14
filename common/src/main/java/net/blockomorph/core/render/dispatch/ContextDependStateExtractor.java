package net.blockomorph.core.render.dispatch;

import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.InPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class ContextDependStateExtractor {

	protected static Object extractRenderStateFor(Object gameObject, float deltaTick, ClientInPlayerManager additionalCtx) {
		return switch (gameObject) {
			case null -> null;
			case PrimedTnt tnt -> fillTnt(tnt, deltaTick);
			case BlockEntity blockEntity -> fillBlockEntity(blockEntity, additionalCtx, deltaTick);
			default -> throw new IncompatibleClassChangeError("Cannot findFluid action for object: " + gameObject + " Type: " + gameObject.getClass().getName());
		};
	}

	private static TntRenderState fillTnt(PrimedTnt tnt, float deltaTick) {
		TntRenderer tntRenderer = (TntRenderer) GuiUtils.MC.getEntityRenderDispatcher().getRenderer(tnt);
		TntRenderState state = tntRenderer.createRenderState();
		try {
			tntRenderer.extractRenderState(tnt, state, deltaTick);
		} catch (Exception ignored) {}
		return state;
	}

	private static MorphedBlockEntity fillBlockEntity(BlockEntity blockEntity, ClientInPlayerManager manager, float deltaTick) {
		if (blockEntity != null) {
			Vec3 camPos = GuiUtils.MC.gameRenderer.getMainCamera().getPosition();
			int brakeProgress = manager.getDestructionHandler().getBestProgress(InPlayerBlockPos.fromDelta(manager.getZeroKey(), blockEntity.getBlockPos()));
			return new MorphedBlockEntity(blockEntity, translateCamPos(camPos, manager), deltaTick, brakeProgress);
		}
		return null;
	}

	private static Vec3 translateCamPos(Vec3 camPos, InPlayerManager manager) {
		BlockPos zero = manager.getZeroKey();
		return new Vec3(
				camPos.x - MorphMath.getRealBlockPosCenter(Direction.Axis.X, manager.getOwner()) + zero.getX(),
				camPos.y - MorphMath.getRealBlockPosCenter(Direction.Axis.Y, manager.getOwner()) + zero.getY(),
				camPos.z - MorphMath.getRealBlockPosCenter(Direction.Axis.Z, manager.getOwner()) + zero.getZ()
		);
	}

	public record MorphedBlockEntity(BlockEntity blockEntity, Vec3 morphCam, float deltaTick, int brakeProgress) {}
}
