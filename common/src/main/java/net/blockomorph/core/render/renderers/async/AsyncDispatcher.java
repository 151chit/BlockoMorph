package net.blockomorph.core.render.renderers.async;

import net.blockomorph.core.render.blockGetter.InPlayerBlockAndTintGetter;
import net.minecraft.core.SectionPos;

public interface AsyncDispatcher {
	InPlayerBlockAndTintGetter makeSnapshot(SectionPos pos);
	AsyncWorkNeed getWorkStatus(SectionPos pos);

	enum AsyncWorkNeed {
		NO, REBAKE, RESORT, YES
	}
}
