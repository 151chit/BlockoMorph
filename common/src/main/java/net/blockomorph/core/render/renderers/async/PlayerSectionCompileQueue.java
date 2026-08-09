package net.blockomorph.core.render.renderers.async;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerSectionCompileQueue {
	private final List<PlayerCompileTask> tasks = new ObjectArrayList<>();

	protected synchronized void schedule(PlayerCompileTask task) {
		this.tasks.add(task);
	}

	protected synchronized PlayerCompileTask poll(Vec3 cameraPos) {
		ListIterator<PlayerCompileTask> iterator = this.tasks.listIterator();
		int bestIndex = -1;
		double bestDistance = Double.MAX_VALUE;

		while(iterator.hasNext()) {
			int taskIndex = iterator.nextIndex();
			double distance = iterator.next().getPos().distanceToSqr(cameraPos);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestIndex = taskIndex;
			}
		}
		if (bestIndex >= 0) {
			return this.tasks.remove(bestIndex);
		}
		return null;
	}

	protected static class PlayerCompileTask {
		private final AsyncBlocksRenderer renderer;
		private final Runnable task;
		private final AtomicBoolean cancelled = new AtomicBoolean();

		protected PlayerCompileTask(AsyncBlocksRenderer renderer, Runnable task) {
			this.renderer = renderer;
			this.task = task;
		}

		protected void cancel() {
			this.cancelled.set(true);
		}

		protected boolean isCancelled() {
			return this.cancelled.get();
		}

		protected void run() {
			this.task.run();
		}

		private Vec3 getPos() {
			return this.renderer.getRootStorage().sectionCenter(this.renderer.getPos(), 0.5f);
		}
	}
}
