package net.blockomorph.screens.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntSupplier;

public class ScrollerManager<T> {
	private static final ResourceLocation SCROLLER_SPRITE = GuiUtils.vanillaRes("container/creative_inventory/scroller");
	private static final ResourceLocation SCROLLER_DISABLED_SPRITE = GuiUtils.vanillaRes("container/creative_inventory/scroller_disabled");
	private static final CustomBarData VANILLA_SCROLLBAR = new CustomBarData(null, 12, 15);
	@Nullable private List<T> list;
	private final IntSupplier barX;
	private final IntSupplier barY;
	private final int row;
	private final int column;
	private final int barHeight;
	private final List<T> renderable;
	protected float scrollOffset = 0; // 0 - 100 %
	private boolean scrollWork;
	private final CustomBarData data;

	public ScrollerManager(IntSupplier barX, IntSupplier barY, int height, int row, int column, List<T> renderable, @Nullable CustomBarData data) {
		this.renderable = renderable;
		this.barHeight = height;
		this.barX = barX;
		this.barY = barY;
		this.row = row;
		this.column = column;
		this.data = data == null ? VANILLA_SCROLLBAR : data;
	}

	public boolean canScroll() {
		if (this.list == null) return false;
		return this.list.size() > this.row * this.column;
	}

	public void renderScroller(GuiUtils gui) {
		if (data == VANILLA_SCROLLBAR) {
			gui.renderSprite(this.canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE, this.barX.getAsInt(), this.barY.getAsInt() + (int) ((float) (this.barHeight - 15) * this.scrollOffset), 12, 15);
		} else {
			gui.blit(
					this.data.barSprite(),
					this.barX.getAsInt(),
					this.barY.getAsInt() + (int) ((float) (this.barHeight - this.data.barHeight) * this.scrollOffset),
					0, this.canScroll() ? 0 : this.data.barHeight,
					this.data.barLength,
					this.data.barHeight,
					this.data.barLength,
					this.data.barHeight * 2
			);
		}
	}

	public void setMainList(@Nullable List<T> main) {
		this.list = main;
	}

	public List<T> getMainList() {
		return this.list;
	}

	public void setScrollOffset(float i) {
		this.scrollOffset = Mth.clamp(i, 0f, 1f);
	}

	public float getScrollerOffset() {
		return this.scrollOffset;
	}

	private int findRowNumber() {
		return Mth.positiveCeilDiv(this.list != null ? this.list.size() : 0, this.row) - this.column;
	}

	protected int findRowIndexForScrollOffset(float f) {
		return Math.max((int) ((double) (f * (float) this.findRowNumber()) + (double) 0.5F), 0);
	}

	public boolean mouseClicked(double x, double y) {
		if (x > this.barX.getAsInt() && x < this.barX.getAsInt() + this.data.barLength + 1 && y > this.barY.getAsInt() && y < this.barY.getAsInt() + this.barHeight) {
			this.scrollWork = this.canScroll();
			return true;
		}
		return false;
	}

	public boolean mouseDragged(double mouseY) {
		if (this.scrollWork) {
			float barHeight = this.data.barHeight;
			float scroll = ((float) mouseY - (float) this.barY.getAsInt() - (barHeight / 2f)) / ((float) this.barHeight - barHeight);
			this.scrollOffset = Mth.clamp(scroll, 0.0F, 1.0F);
			this.refreshList();
			return true;
		}
		return false;
	}

	public void disableScrollWork() {
		this.scrollWork = false;
	}

	public boolean mouseScrolled(double yScrolled) {
		if (!this.canScroll()) return false;
		this.scrollOffset = Mth.clamp(this.scrollOffset - (float) (yScrolled / (double) this.findRowNumber()), 0.0F, 1.0F);
		this.refreshList();
		return true;
	}

	public void refreshList() {
		this.renderable.clear();
		if (this.list != null && !this.list.isEmpty()) {
			int i = this.findRowIndexForScrollOffset(this.scrollOffset);
			int startIndex = i * this.row;

			for (int j = 0; j < this.column; ++j) {
				for (int k = 0; k < this.row; ++k) {
					int index = k + j * this.row + startIndex;

					if (index < this.list.size()) {
						this.renderable.add(this.list.get(index));
					} else break;
				}
			}
		}
	}

	public record CustomBarData(ResourceLocation barSprite, int barLength, int barHeight) {
	}
}
