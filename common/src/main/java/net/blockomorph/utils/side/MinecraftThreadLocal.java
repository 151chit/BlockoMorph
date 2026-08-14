package net.blockomorph.utils.side;

import java.util.function.Supplier;

public class MinecraftThreadLocal<T> {
	private final ThreadLocal<T> emergencyFallback;
	private T server;
	private T client;
	private final Supplier<T> factory;

	public MinecraftThreadLocal(boolean useFallback, Supplier<T> factory) {
		if (useFallback) {
			this.emergencyFallback = new ThreadLocal<>();
		} else
			this.emergencyFallback = null;
		this.factory = factory;
	}

	public T get() {
		Side side = Side.get();
		T val = switch (side) {
			case CLIENT -> this.client;
			case SERVER -> this.server;
			case UNKNOWN -> this.getByFallback();
		};
		if (val == null && this.factory != null && (side != Side.UNKNOWN || this.emergencyFallback != null)) {
			T newVal = this.factory.get();
			val = newVal;
			this.set(newVal);
		}
		return val;
	}

	private T getByFallback() {
		if (this.emergencyFallback != null) return this.emergencyFallback.get();
		return null;
	}

	public void set(T value) {
		switch (Side.get()) {
			case CLIENT -> this.client = value;
			case SERVER -> this.server = value;
			case UNKNOWN -> this.setByFallback(value);
		}
	}

	private void setByFallback(T value) {
		if (this.emergencyFallback != null)
			this.emergencyFallback.set(value);
	}
}
