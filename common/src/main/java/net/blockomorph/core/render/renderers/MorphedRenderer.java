package net.blockomorph.core.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.core.render.utils.SortedRenderOutput;
import net.blockomorph.core.render.dispatch.MorphedPlayerRenderState;

public interface MorphedRenderer {
	MorphedRenderer DUMMY = (ignore1, ignore2, ignore3) -> {};
	void submit(PoseStack posestack, MorphedPlayerRenderState renderState, SortedRenderOutput collector);
}
