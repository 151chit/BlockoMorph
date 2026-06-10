package net.blockomorph.screens.utils;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GuiBlockRenderState implements PictureInPictureRenderState {
	private final int[] rectangle = new int[4];
	private final ScreenRectangle bounds;
	private final ScreenRectangle scissorsArea;
	private final float deltaTick;
	private final float size;
	private final BlockState state;
	private final BlockEntity entity;

	public GuiBlockRenderState(BlockState blockState, @Nullable BlockEntity blockEntity, int x, int y, float scale, float deltaTick, ScreenRectangle scissorsArea) {
		this.deltaTick = deltaTick;
		int SCISSORS_SIZE = 100;
		this.rectangle[0] = x - SCISSORS_SIZE;
		this.rectangle[1] = x + SCISSORS_SIZE;
		this.rectangle[2] = y - SCISSORS_SIZE;
		this.rectangle[3] = y + SCISSORS_SIZE;
		this.size = scale;
		this.scissorsArea = scissorsArea;
		this.bounds = PictureInPictureRenderState.getBounds(this.x0(), this.y0(), this.x1(), this.y1(), scissorsArea);
		this.state = blockState;
		this.entity = blockEntity;
	}

	@Override
	public int x0() {
		return this.rectangle[0];
	}

	@Override
	public int x1() {
		return this.rectangle[1];
	}

	@Override
	public int y0() {
		return this.rectangle[2];
	}

	@Override
	public int y1() {
		return this.rectangle[3];
	}

	@Override
	public float scale() {
		return this.size;
	}

	public float getDeltaTick() {
		return this.deltaTick;
	}

	public BlockState getState() {
		return this.state;
	}

	public BlockEntity getBlockEntity() {
		return this.entity;
	}

	@Override
	public ScreenRectangle scissorArea() {
		return this.scissorsArea;
	}

	@Override
	public ScreenRectangle bounds() {
		return this.bounds;
	}
}
