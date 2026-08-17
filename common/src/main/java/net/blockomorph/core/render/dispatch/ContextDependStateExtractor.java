package net.blockomorph.core.render.dispatch;

import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContextDependStateExtractor {

	protected static Object extractRenderStateFor(Object gameObject, float deltaTick, ClientInPlayerManager additionalCtx) {
		return switch (gameObject) {
			case null -> null;
			case PrimedTnt tnt -> fillTnt(tnt, deltaTick);
			case BlockEntity blockEntity -> fillBlockEntity(blockEntity, additionalCtx, deltaTick);
			default -> throw new IncompatibleClassChangeError("Cannot findFluid action for object: " + gameObject + " Type: " + gameObject.getClass().getName());
		};
	}

	@SuppressWarnings("rawtypes")
	private static TntStateWithRenderer fillTnt(PrimedTnt tnt, float deltaTick) {
		TntRenderer tntRenderer = (TntRenderer) GuiUtils.MC.getEntityRenderDispatcher().getRenderer(tnt);
		TntRenderState state = tntRenderer.createRenderState();
		try {
			tntRenderer.extractRenderState(tnt, state, deltaTick);
		} catch (Exception ignored) {}
		return new TntStateWithRenderer<>(state, tntRenderer);
	}

	private static MorphedBlockEntity fillBlockEntity(BlockEntity blockEntity, ClientInPlayerManager manager, float deltaTick) {
		if (blockEntity != null) {
			int brakeProgress = manager.getDestructionHandler().getBestProgress(InPlayerBlockPos.fromDelta(manager.getZeroKey(), blockEntity.getBlockPos()));
			return new MorphedBlockEntity(blockEntity, deltaTick, brakeProgress);
		}
		return null;
	}

	public record MorphedBlockEntity(BlockEntity blockEntity, float deltaTick, int brakeProgress) {}

	public record TntStateWithRenderer<STATE extends TntRenderState, RENDERER extends TntRenderer>(STATE tntRenderState, RENDERER renderer) {}
}
