package net.blockomorph.core.render.renderers.async;

import com.google.common.util.concurrent.MoreExecutors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blockomorph.core.render.layers.PlayerSectionLayerGroup;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PlayersAsyncBakersManager implements AutoCloseable {
	private final Map<UUID, AsyncRenderersStorage> renderers = new Object2ObjectOpenHashMap<>();
	private final PlayerSectionCompileQueue queue = new PlayerSectionCompileQueue();
	private final AtomicReference<Vec3> cameraPos = new AtomicReference<>(Vec3.ZERO);
	private final ExecutorService executor;
	private final boolean needShutdownExecutor;

	public PlayersAsyncBakersManager(ExecutorService executor, boolean needShutdownExecutor) {
		this.executor = executor;
		this.needShutdownExecutor = needShutdownExecutor;
	}

	public void setCameraPos(Vec3 cameraPos) {
		this.cameraPos.set(cameraPos);
	}

	public Vec3 cameraPos() {
		return this.cameraPos.get();
	}

	public AsyncRenderersStorage allocateOrGetFor(UUID ownerId) {
		return this.renderers.computeIfAbsent(ownerId, AsyncRenderersStorage::new);
	}

	public void terminateFor(UUID ownerId) {
		var renderer = this.renderers.remove(ownerId);
		if (renderer != null) renderer.destroy();
	}

	public void drawOnGpu(ClientLevel level, PlayerSectionLayerGroup layers, float deltaTick, Frustum frustum) {
		if (level == null) return;
		var players = level.players();
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			if (player.isRemoved()) continue;
			var renderer = this.renderers.get(player.getUUID());
			if (renderer != null) renderer.drawOnGpu(layers, this.cameraPos.get(), deltaTick, frustum);
		}
	}

	public void clearAllPlayers() {
		this.renderers.values().forEach(AsyncRenderersStorage::destroy);
		this.renderers.clear();
	}

	@Override
	public void close() {
		this.clearAllPlayers();
		if (this.needShutdownExecutor)
			this.closePool();
	}

	private void closePool() {
		this.executor.shutdown();
		boolean terminated;
		try {
			terminated = this.executor.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) { terminated = false; }
		if (!terminated) this.executor.shutdownNow();
	}

	protected void scheduleCompileTask(PlayerSectionCompileQueue.PlayerCompileTask task) {
		this.queue.schedule(task);
		this.executor.execute(() -> {
			var bestTask = this.queue.poll(this.cameraPos.get());
			if (bestTask != null) bestTask.run();
		});
	}

	@FunctionalInterface
	public interface Provider {
		PlayersAsyncBakersManager getBaker();
		static PlayersAsyncBakersManager getFromVanilla() {
			return ((Provider) GuiUtils.MC.levelRenderer).getBaker();
		}
	}

	public static ExecutorService createPool() {
		int threads = Util.maxAllowedExecutorThreads();
		ExecutorService executor;
		if (threads <= 0) {
			executor = MoreExecutors.newDirectExecutorService();
		} else {
			AtomicInteger number = new AtomicInteger(1);
			executor = new ForkJoinPool(threads, pool -> {
				ForkJoinWorkerThread thread = new MorphBakerThread(pool) {
					@Override
					protected void onTermination(Throwable exception) {
						error(this, exception);
					}
				};
				thread.setName(MorphUtils.MODID + "-player-sections-baker-" + number.getAndIncrement());
				return thread;
			}, PlayersAsyncBakersManager::error, true);
		}
		return executor;
	}

	public static class MorphBakerThread extends ForkJoinWorkerThread {//for detect for FRAPI
		private MorphBakerThread(ForkJoinPool pool) {
			super(pool);
		}
	}

	private static void error(Thread thread, Throwable exception) {
		if (exception != null)
			MorphUtils.LOGGER.error("Failed to bake player section async. Thread: {}", thread.getName(), exception);
	}
}
