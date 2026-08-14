package net.blockomorph.utils.side;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;

import java.util.ConcurrentModificationException;

public enum Side {
	CLIENT,
	SERVER,
	UNKNOWN {@Override public void mark(BlockableEventLoop<? extends Runnable> instance) {
		throw new UnsupportedOperationException("Mark thread for unknown thread group!");
	}};

	private Thread currThread;
	private BlockableEventLoop<? extends Runnable> instance;
	public void mark(BlockableEventLoop<? extends Runnable> instance) {
		if (instance != null) {
			this.currThread = Thread.currentThread();
			this.instance = instance;
		} else {
			this.currThread = null;
			this.instance = null;
		}
	}

	public BlockableEventLoop<? extends Runnable> getInstance() {
		return this.instance;
	}

	public boolean isThisSide(Thread thread) {
		return this.currThread == thread;
	}

	public static Side get() {
		Thread thread = Thread.currentThread();
		if (CLIENT.isThisSide(thread)) return CLIENT;
		if (SERVER.isThisSide(thread)) return SERVER;
		return UNKNOWN;
	}

	public static void assertOnGameThread(Object additional) {
		if (get() == UNKNOWN)
			throw new ConcurrentModificationException("Thread:" + Thread.currentThread().getName() + additional);
	}

	public static MinecraftServer serverOrThrow() {
		var handler = get().getInstance();
		if (handler instanceof MinecraftServer sv) return sv;
		throw new IllegalStateException("Server not run!");
	}
}

