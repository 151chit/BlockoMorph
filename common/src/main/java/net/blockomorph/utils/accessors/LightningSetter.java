package net.blockomorph.utils.accessors;

import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

public interface LightningSetter {
	void runWithLight(Runnable rendering, Vector3f start, Vector3f end);

	void disable();

	static void renderWithLight(Runnable rendreable, Vector3f start, Vector3f end) {
		getInstance().runWithLight(rendreable, start, end);
	}

	static void disableLigth() {
		getInstance().disable();
	}

	private static LightningSetter getInstance() {
		return (LightningSetter) Minecraft.getInstance().gameRenderer.getLighting();
	}
}
