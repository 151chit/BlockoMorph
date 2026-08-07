package net.blockomorph.core.render.renderers.async;

import net.blockomorph.core.HitBoxCalculator;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.render.dispatch.SectionsStateExtractor;
import net.blockomorph.core.render.layers.PlayerSectionLayer;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncRenderersStorage {
	private static final Vec3 INFINITY = new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	private final AsyncBlocksRenderer[] renderers = new AsyncBlocksRenderer[8];
	private final AtomicReference<PlayerData> posAndBounds = new AtomicReference<>(null);
	private final AsyncBlocksRenderer[] renderersSorted = new AsyncBlocksRenderer[8];
	private PlayerAccessor player;
	private boolean destroy;

	protected AsyncRenderersStorage(UUID ownerId) {
		SectionsStateExtractor.POSES.forEach(pos -> this.renderers[MorphMath.offsetSectionIndex(pos)] = new AsyncBlocksRenderer(ownerId, pos, this));
		System.arraycopy(this.renderers, 0, this.renderersSorted, 0, 8);
	}

	private record PlayerData(Vec3 oldPos, HitBoxCalculator.HitboxData hitboxData) {
		PlayerData(PlayerAccessor pl) {
			this(pl.player().oldPosition(), new HitBoxCalculator.HitboxData(pl.player().position(), pl.minPos(), pl.maxPos()));
		}
	}

	public AsyncBlocksRenderer.ImmediateRenderState checkSection(AsyncDispatcher dispatcher, SectionPos offset) {
		int sectionIndex = MorphMath.offsetSectionIndex(offset);
		if (sectionIndex == -1) return null;
		return this.renderers[sectionIndex].checkAsyncAndGetWorkStatus(dispatcher);
	}

	protected void drawOnGpu(PlayerSectionLayerGroup layers, Vec3 camPos, float deltaTick) {
		if (this.player != null && this.player.player().isRemoved()) this.player = null;
		if (!this.shouldRender()) return;
		AsyncBlocksRenderer[] forRender = this.renderers;
		for (PlayerSectionLayer layer : layers.layers()) {
			if (layer.isTranslucent()) {
				Arrays.sort(this.renderersSorted, (renderer1, renderer2) ->
						Double.compare(
								this.sectionCenter(renderer2.getPos(), deltaTick).distanceToSqr(camPos),
								this.sectionCenter(renderer1.getPos(), deltaTick).distanceToSqr(camPos))
				);
				forRender = this.renderersSorted;
				break;
			}
		}
		for (AsyncBlocksRenderer renderer : forRender) {
			renderer.draw(layers, camPos, deltaTick);
		}
	}

	private boolean shouldRender() {
		var localPlayer = GuiUtils.MC.player;
		var camera = GuiUtils.MC.gameRenderer.getMainCamera();
		var entityRenderer = GuiUtils.MC.getEntityRenderDispatcher();

		if (this.player == null || localPlayer == null) return false;

		boolean base = entityRenderer.shouldRender(
				this.player.player(), camera.getCullFrustum(), camera.position().x, camera.position().y, camera.position().z) ||
				this.player.player().hasIndirectPassenger(localPlayer);
		if (!base) return false;

		boolean camCondition = this.player != camera.entity() || camera.isDetached() || camera.entity() instanceof LivingEntity lv && lv.isSleeping();
		if (!camCondition) return false;

		return !(this.player instanceof LocalPlayer) || camera.entity() == this.player || this.player == localPlayer && !localPlayer.isSpectator();
	}

	protected Vec3 sectionCenter(SectionPos pos, float deltaTick) {
		PlayerData data = this.posAndBounds.get();
		if (data != null) {
			double deltaX = Mth.lerp(deltaTick, data.oldPos.x, data.hitboxData.position().x);
			double deltaY = Mth.lerp(deltaTick, data.oldPos.y, data.hitboxData.position().y);
			double deltaZ = Mth.lerp(deltaTick, data.oldPos.z, data.hitboxData.position().z);
			int blockX = SectionPos.sectionToBlockCoord(pos.x(), 8);
			int blockY = SectionPos.sectionToBlockCoord(pos.y(), 8);
			int blockZ = SectionPos.sectionToBlockCoord(pos.z(), 8);
			return new Vec3(
					MorphMath.getRealBlockPosAxis(Direction.Axis.X, deltaX, data.hitboxData.minPos(), data.hitboxData.maxPos(), blockX),
					MorphMath.getRealBlockPosAxis(Direction.Axis.Y, deltaY, data.hitboxData.minPos(), data.hitboxData.maxPos(), blockY),
					MorphMath.getRealBlockPosAxis(Direction.Axis.Z, deltaZ, data.hitboxData.minPos(), data.hitboxData.maxPos(), blockZ)
			);
		}
		return INFINITY;
	}

	protected PlayerAccessor playerOwner() {
		return this.player;
	}

	public void appendPlayer(PlayerAccessor pl) {
		if (pl != null && !pl.player().isRemoved()) {
			this.player = pl;
			this.posAndBounds.set(new PlayerData(pl));
		}
	}

	public boolean destroyed() {
		return this.destroy;
	}

	protected void destroy() {
		for (AsyncBlocksRenderer renderer : this.renderers)
			renderer.destroyAll();
		this.destroy = true;
	}
}
