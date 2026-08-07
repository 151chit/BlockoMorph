package net.blockomorph.core.serialization;

public interface DataWorker {
	void tick();
	boolean isDone();
	void run();
}
