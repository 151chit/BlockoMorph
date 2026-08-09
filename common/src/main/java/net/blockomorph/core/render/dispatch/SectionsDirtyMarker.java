package net.blockomorph.core.render.dispatch;

import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.HitBoxCalculator;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.minecraft.core.SectionPos;

import java.util.Arrays;

public class SectionsDirtyMarker {
	private final boolean[] sectionsDirty = new boolean[8];
	private final SectionPos zeroKeySection;
	private final ClientInPlayerManager manager;
	private HitBoxCalculator.HitboxData oldPos;

	public SectionsDirtyMarker(ClientInPlayerManager manager) {
		this.manager = manager.assertOnInit();
		this.zeroKeySection = SectionPos.of(manager.getZeroKey());
	}

	public void tick() {
		if (this.manager.getOwner() == null) return;
		var data = MorphMath.isBlockPosChanged(this.oldPos, this.manager.getOwner());
		if (data != null) {
			this.oldPos = data;
			Arrays.fill(this.sectionsDirty, true);
		}
	}

	public boolean isSectionDirty(SectionPos offset) {
		int index = MorphMath.offsetSectionIndex(offset);
		if (index == -1) return false;
		return this.sectionsDirty[index];
	}

	public void markSectionDirty(SectionPos offset, boolean yes) {
		int index = MorphMath.offsetSectionIndex(offset);
		if (index == -1) return;
		this.sectionsDirty[index] = yes;
	}

	public void markSectionDirty(int inPlayerBlockPos) {
		if (!InPlayerBlockPos.isValid(inPlayerBlockPos)) return;
		int sectionX = SectionPos.blockToSectionCoord(InPlayerBlockPos.getX(inPlayerBlockPos));
		int sectionY = SectionPos.blockToSectionCoord(InPlayerBlockPos.getY(inPlayerBlockPos));
		int sectionZ = SectionPos.blockToSectionCoord(InPlayerBlockPos.getZ(inPlayerBlockPos));
		int index = MorphMath.offsetSectionIndex(sectionX, sectionY, sectionZ);
		if (index == -1) return;
		this.sectionsDirty[index] = true;
	}

	public void markSectionDirty(InPlayerBlockPos pos) {
		this.markSectionDirty(pos.asInt());
	}

	public void markSectionDirty(int keyX, int keyY, int keyZ, boolean yes) {
		int sectionX = keyX - this.zeroKeySection.x() - 1;
		int sectionY = keyY - this.zeroKeySection.y();
		int sectionZ = keyZ - this.zeroKeySection.z() - 1;
		int index = MorphMath.offsetSectionIndex(sectionX, sectionY, sectionZ);
		if (index == -1) return;
		this.sectionsDirty[index] = yes;
	}
}
